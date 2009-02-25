package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

/**
 * Class that enables the support for Drag & Drop.
 * <p/>
 * Limitations:
 * This handler only supports {@link JTree} with an {@link javax.swing.tree.TreeModel model}
 * which is backed by {@link Layer layer}.
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class LayerTreeTransferHandler extends TransferHandler {

    private static final DataFlavor layerFlavor = new DataFlavor(LayerContainer.class, "LayerContainer");
    private final JTree tree;

    LayerTreeTransferHandler(JTree tree) {
        this.tree = tree;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(layerFlavor) &&
               support.isDrop() &&
               support.getComponent() == tree;
    }

    @Override
    public boolean importData(TransferSupport support) {
        final Transferable transferable = support.getTransferable();

        final LayerContainer transferLayer;
        try {
            transferLayer = (LayerContainer) transferable.getTransferData(layerFlavor);
        } catch (UnsupportedFlavorException ignore) {
            return false;
        } catch (IOException ignore) {
            return false;
        }

        if (!isValidDrag(support, transferLayer)) {
            return false;
        }

        // remove and add for Drag&Drop
        // cannot use exportDone(Transferable); layer has to be removed first and then added,
        // because the parent of the layer is set to null when removing
        removeLayer(transferLayer);
        addLayer(transferLayer, support);
        return true;
    }



    @Override
    public int getSourceActions(JComponent component) {
        return component == tree ? MOVE : NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent component) {
        if (component == tree) {
            final TreePath treePath = tree.getSelectionPath();
            final Layer draggedLayer = (Layer) treePath.getLastPathComponent();
            final Layer oldParentLayer = (Layer) treePath.getParentPath().getLastPathComponent();
            final int oldChildIndex = oldParentLayer.getChildIndex(draggedLayer.getId());
            return new LayerTransferable(draggedLayer, oldParentLayer, oldChildIndex);
        }
        return null;
    }

    private static void addLayer(LayerContainer layerContainer, TransferSupport support) {
        Layer transferLayer = layerContainer.getDraggedLayer();
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath treePath = dropLocation.getPath();
        Layer targetLayer = (Layer) treePath.getLastPathComponent();
        List<Layer> targetList = targetLayer.getChildren();

        int targetIndex = dropLocation.getChildIndex();
        if (targetIndex == -1) {  // moving into target layer
            targetIndex = 0;    // insert at the beginning
        }

        if (targetList.size() <= targetIndex) {
            targetList.add(transferLayer);
        } else {
            targetList.add(targetIndex, transferLayer);
        }

        final JTree tree = (JTree) support.getComponent();
        final TreePath newTreePath = treePath.pathByAddingChild(transferLayer);
        tree.makeVisible(newTreePath);
        tree.scrollPathToVisible(newTreePath);
    }

    private static void removeLayer(LayerContainer layerContainer) {
        final Layer oldParentayer = layerContainer.getOldParentLayer();
        final int oldIndex = layerContainer.getOldChildIndex();
        oldParentayer.getChildren().remove(oldIndex);
    }

    private static boolean isValidDrag(TransferSupport support, LayerContainer layerContainer) {
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath treePath = dropLocation.getPath();

        final Object[] path = treePath.getPath();
        for (Object o : path) {
            final Layer layer = (Layer) o;
            if (layer == layerContainer.getDraggedLayer()) {
                return false;
            }
        }
        return true;
    }

    private static class LayerTransferable implements Transferable {

        private LayerContainer layerContainer;

        private LayerTransferable(Layer draggedLayer, Layer oldParentLayer, int oldChildIndex) {
            layerContainer = new LayerContainer(draggedLayer, oldParentLayer, oldChildIndex);

        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{layerFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(layerFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!flavor.equals(layerFlavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return layerContainer;
        }

    }

    private static class LayerContainer {

        private final Layer draggedLayer;
        private final Layer oldParentLayer;
        private final int oldChildIndex;

        private LayerContainer(Layer draggedLayer, Layer oldParentLayer, int oldChildIndex) {

            this.draggedLayer = draggedLayer;
            this.oldParentLayer = oldParentLayer;
            this.oldChildIndex = oldChildIndex;
        }

        public Layer getDraggedLayer() {
            return draggedLayer;
        }

        public Layer getOldParentLayer() {
            return oldParentLayer;
        }

        public int getOldChildIndex() {
            return oldChildIndex;
        }
    }
}
