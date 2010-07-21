/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.product.ProductSceneView;

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
    private final ProductSceneView view;
    private final JTree tree;

    LayerTreeTransferHandler(ProductSceneView view, JTree tree) {
        this.view = view;
        this.tree = tree;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
        TreePath treePath = dropLocation.getPath();
        Layer targetLayer = (Layer) treePath.getLastPathComponent();
        int targetIndex = dropLocation.getChildIndex();
        boolean moveAllowed = true;
        if (targetIndex == -1) { //  -1 indicates move into other layer
            moveAllowed = targetLayer.isCollectionLayer();
        }
        return support.isDataFlavorSupported(layerFlavor) &&
               support.isDrop() &&
               support.getComponent() == tree &&
               moveAllowed;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (support.getComponent() != tree) {
            return false;
        }

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
            final Layer draggedLayer = view.getSelectedLayer();
            final Layer oldParentLayer = draggedLayer.getParent();
            final int oldChildIndex = oldParentLayer.getChildIndex(draggedLayer.getId());
            return new LayerTransferable(draggedLayer, oldParentLayer, oldChildIndex);
        }
        return null;
    }

    private void addLayer(LayerContainer layerContainer, TransferSupport support) {
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

        Layer targetLayer = (Layer) treePath.getLastPathComponent();
        int targetIndex = dropLocation.getChildIndex();
        if (targetIndex == -1) { //  -1 indicates move into other layer
            return targetLayer.isCollectionLayer();
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
