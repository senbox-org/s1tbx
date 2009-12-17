package org.esa.beam.visat;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.TreeSelectionContext;
import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.actions.ShowMetadataViewAction;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The tool window which displays the tree of open products.
 */
public class ProductsToolView extends AbstractToolView {

    public static final String ID = ProductsToolView.class.getName();

    /**
     * Product tree of the application
     */
    private ProductTree productTree;
    private VisatApp visatApp;
    private TreeSelectionContext selectionContext;

    public ProductsToolView() {
        this.visatApp = VisatApp.getApp();
        // We need product tree early, otherwise the application cannot add ProductTreeListeners
        initProductTree();
    }

    public ProductTree getProductTree() {
        return productTree;
    }

    @Override
    public JComponent createControl() {
        final JScrollPane productTreeScrollPane = new JideScrollPane(productTree); // <JIDE>
        productTreeScrollPane.setPreferredSize(new Dimension(320, 480));
        productTreeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        productTreeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        productTreeScrollPane.setBorder(null);
        productTreeScrollPane.setViewportBorder(null);

        return productTreeScrollPane;
    }

    private void initProductTree() {
        productTree = new ProductTree();
        productTree.addProductTreeListener(new VisatPTL());
        productTree.setCommandManager(visatApp.getCommandManager());
        productTree.setCommandUIFactory(visatApp.getCommandUIFactory());
        visatApp.getProductManager().addListener(new ProductManagerL());
        visatApp.addInternalFrameListener(new SceneViewListener());
        selectionContext = new ProductTreeSelectionContext(productTree);
    }

    /**
     * Gets the current selection context, if any.
     *
     * @return The current selection context, or {@code null} if none exists.
     * @since BEAM 4.7
     */
    @Override
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    private void registerActiveVectorDataNode(ProductSceneView sceneView) {
        VectorDataNode vectorDataNode = getSelectedVectorDataNode(sceneView);
        if (vectorDataNode != null) {
            productTree.registerActiveProductNodes(vectorDataNode);
        }
    }

    private void deregisterActiveVectorDataNode(ProductSceneView sceneView) {
        VectorDataNode vectorDataNode = getSelectedVectorDataNode(sceneView);
        if (vectorDataNode != null) {
            productTree.deregisterActiveProductNodes(vectorDataNode);
        }
    }

    private static VectorDataNode getSelectedVectorDataNode(ProductSceneView sceneView) {
        final Layer layer = sceneView.getSelectedLayer();
        if (layer instanceof VectorDataLayer) {
            final VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
            return vectorDataLayer.getVectorDataNode();
        } else {
            return null;
        }
    }

    private void setSelectedVectorDataNode(VectorDataNode vectorDataNode) {
        final ProductSceneView sceneView = visatApp.getSelectedProductSceneView();
        if (sceneView != null) {
            setSelectedVectorDataNode(sceneView, vectorDataNode);
        }
    }

    private static void setSelectedVectorDataNode(ProductSceneView sceneView, final VectorDataNode vectorDataNode) {
        final LayerFilter layerFilter = new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer instanceof VectorDataLayer && ((VectorDataLayer) layer).getVectorDataNode() == vectorDataNode;
            }
        };
        Layer layer = LayerUtils.getChildLayer(sceneView.getRootLayer(), layerFilter, LayerUtils.SearchMode.DEEP);
        if (layer != null) {
            sceneView.setSelectedLayer(layer);
        }
    }

    /**
     * This listener listens to product tree events in VISAT's product browser.
     */
    private class VisatPTL extends ProductTreeListenerAdapter {

        public VisatPTL() {
        }

        @Override
        public void productAdded(final Product product) {
            Debug.trace("VisatApp: product added: " + product.getDisplayName());
            setSelectedProductNode(product);
        }

        @Override
        public void productRemoved(final Product product) {
            Debug.trace("VisatApp: product removed: " + product.getDisplayName());
            if (visatApp.getSelectedProduct() != null && visatApp.getSelectedProduct() == product) {
                visatApp.setSelectedProductNode((ProductNode) null);
            } else {
                visatApp.updateState();
            }
        }

        @Override
        public void productSelected(final Product product, final int clickCount) {
            setSelectedProductNode(product);
        }

        @Override
        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            rasterDataNodeSelected(tiePointGrid, clickCount);
        }

        @Override
        public void bandSelected(final Band band, final int clickCount) {
            rasterDataNodeSelected(band, clickCount);
        }

        @Override
        public void vectorDataSelected(final VectorDataNode vectorDataNode, int clickCount) {
            setSelectedProductNode(vectorDataNode);
            setSelectedVectorDataNode(vectorDataNode);
        }

        @Override
        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            setSelectedProductNode(group);
            final JInternalFrame frame = visatApp.findInternalFrame(group);
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // ok
                }
                return;
            }
            if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand(ShowMetadataViewAction.ID);
                command.execute(group);
            }
        }

        private void rasterDataNodeSelected(final RasterDataNode raster, final int clickCount) {
            setSelectedProductNode(raster);
            final JInternalFrame[] internalFrames = visatApp.findInternalFrames(raster);
            JInternalFrame frame = null;
            for (final JInternalFrame internalFrame : internalFrames) {
                final int numRasters = ((ProductSceneView) internalFrame.getContentPane()).getNumRasters();
                if (numRasters == 1) {
                    frame = internalFrame;
                    break;
                }
            }
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // ok
                }
            } else if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand("showImageView");
                command.execute(clickCount);
            }
        }


        private void setSelectedProductNode(ProductNode product) {
            //deselectInternalFrame();
            visatApp.setSelectedProductNode(product);
        }

        private void deselectInternalFrame() {
            try {
                final JInternalFrame frame = visatApp.getSelectedInternalFrame();
                if (frame != null) {
                    frame.setSelected(false);
                }
            } catch (PropertyVetoException ignore) {
            }
        }
    }

    static class ProductTreeSelectionContext extends TreeSelectionContext {
        ProductTreeSelectionContext(ProductTree tree) {
            super(tree);
        }

        @Override
        public boolean canDeleteSelection() {
            Object selectedObject = getSelectedObject();
            return isDeletableRasterData(selectedObject)
                    || isDeletableVectorData(selectedObject);
        }

        private boolean isDeletableVectorData(Object selectedObject) {
            if (selectedObject instanceof VectorDataNode) {
                final VectorDataNode vectorDataNode = (VectorDataNode) selectedObject;
                return vectorDataNode.getProduct().isInternalNode(vectorDataNode);
            }
            return false;
        }

        private boolean isDeletableRasterData(Object selectedObject) {
            return selectedObject instanceof Band
                    || selectedObject instanceof TiePointGrid;
        }

        @Override
        public void deleteSelection() {
            Object selectedObject = getSelectedObject();
            if (isDeletableRasterData(selectedObject)) {
                deleteRasterData((RasterDataNode) selectedObject);
            } else if (isDeletableVectorData(selectedObject)) {
                deleteVectorData((VectorDataNode) selectedObject);
            }
        }

        private void deleteVectorData(VectorDataNode vectorDataNode) {
            String message = MessageFormat.format("Do you really want to delete the geometry ''{0}''?\nThis action cannot be undone.\n\n", vectorDataNode.getName());
            int status = VisatApp.getApp().showQuestionDialog("Delete Geometry",
                                                              message, null);
            if (status == JOptionPane.YES_OPTION) {
                Product product = vectorDataNode.getProduct();
                product.getVectorDataGroup().remove(vectorDataNode);
            }
        }

        private void deleteRasterData(RasterDataNode raster) {
            String[] virtualBands = getReferencedVirtualBands(raster);
            String[] validMaskNodes = getReferencedValidMasks(raster);
            String[] masks = getReferencedMasks(raster);

            String message = MessageFormat.format("Do you really want to delete the raster data ''{0}''?\nThis action cannot be undone.\n\n", raster.getName());
            if (virtualBands.length > 0
                    || validMaskNodes.length > 0
                    || masks.length > 0) {
                message += "The raster to be deleted is referenced by\n"; /*I18N*/
            }
            String indent = "    ";
            if (virtualBands.length > 0) {
                message += "the expression of virtual band(s):\n"; /*I18N*/
                for (String virtualBand : virtualBands) {
                    message += indent + virtualBand + "\n";
                }
            }
            if (validMaskNodes.length > 0) {
                message += "the valid-mask expression of band(s) or tie-point grid(s)\n"; /*I18N*/
                for (String validMaskNode : validMaskNodes) {
                    message += indent + validMaskNode + "\n";
                }
            }
            if (masks.length > 0) {
                message += "the mask(s):\n"; /*I18N*/
                for (String mask : masks) {
                    message += indent + mask + "\n";
                }
            }

            final int status = VisatApp.getApp().showQuestionDialog("Delete Raster Data",
                                                                    message, null);
            if (status == JOptionPane.YES_OPTION) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(raster);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
                if (raster.hasRasterData()) {
                    raster.unloadRasterData();
                }
                final Product product = raster.getProduct();
                if (raster instanceof Mask) {
                    product.getMaskGroup().remove((Mask) raster);
                } else if (raster instanceof Band) {
                    product.removeBand((Band) raster);
                } else if (raster instanceof TiePointGrid) {
                    product.removeTiePointGrid((TiePointGrid) raster);
                }
            }
        }

        private Object getSelectedObject() {
            TreePath treePath = (TreePath) getSelection().getSelectedValue();
            return ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
        }

        private static String[] getReferencedValidMasks(final RasterDataNode node) {
            final Product product = node.getProduct();
            final List<String> namesList = new ArrayList<String>();
            if (product != null) {
                for (int i = 0; i < product.getNumBands(); i++) {
                    final Band band = product.getBandAt(i);
                    if (band != node) {
                        if (isNodeReferencedByExpression(node, band.getValidPixelExpression())) {
                            namesList.add(band.getName());
                        }
                    }
                }
                for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                    final TiePointGrid tiePointGrid = product.getTiePointGridAt(i);
                    if (tiePointGrid != node) {
                        if (isNodeReferencedByExpression(node, tiePointGrid.getValidPixelExpression())) {
                            namesList.add(tiePointGrid.getName());
                        }
                    }
                }
            }
            return namesList.toArray(new String[namesList.size()]);
        }

        private static String[] getReferencedMasks(final RasterDataNode node) {
            final Product product = node.getProduct();
            final List<String> namesList = new ArrayList<String>();
            if (product != null) {
                final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
                for (final Mask mask : masks) {
                    final String expression;
                    if (mask.getImageType() instanceof Mask.BandMathType) {
                        expression = Mask.BandMathType.getExpression(mask);
                    } else if (mask.getImageType() instanceof Mask.RangeType) {
                        expression = Mask.RangeType.getRasterName(mask);
                    } else {
                        expression = null;
                    }
                    if (isNodeReferencedByExpression(node, expression)) {
                        namesList.add(mask.getName());
                    }
                }
            }
            return namesList.toArray(new String[namesList.size()]);
        }

        private static String[] getReferencedVirtualBands(final RasterDataNode node) {
            final Product product = node.getProduct();
            final List<String> namesList = new ArrayList<String>();
            if (product != null) {
                for (int i = 0; i < product.getNumBands(); i++) {
                    final Band band = product.getBandAt(i);
                    if (band instanceof VirtualBand) {
                        final VirtualBand virtualBand = (VirtualBand) band;
                        if (isNodeReferencedByExpression(node, virtualBand.getExpression())) {
                            namesList.add(virtualBand.getName());
                        }
                    }
                }
            }
            return namesList.toArray(new String[namesList.size()]);
        }

        @SuppressWarnings({"SimplifiableIfStatement"})
        private static boolean isNodeReferencedByExpression(RasterDataNode node, String expression) {
            if (expression == null || expression.trim().isEmpty()) {
                return false;
            }

            return expression.matches(".*\\b" + node.getName() + "\\b.*");
        }
    }

    private class ProductManagerL implements ProductManager.Listener {
        @Override
        public void productAdded(final ProductManager.Event event) {
            productTree.addProduct(event.getProduct());
            visatApp.getApplicationPage().showToolView(ID);
        }

        @Override
        public void productRemoved(final ProductManager.Event event) {
            final Product product = event.getProduct();
            productTree.removeProduct(product);
            if (visatApp.getSelectedProduct() == product) {
                visatApp.setSelectedProductNode((ProductNode) null);
            }
        }
    }

    private class SelectedLayerListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final Layer oldLayer = (Layer) evt.getOldValue();
            final Layer newLayer = (Layer) evt.getNewValue();
            if (oldLayer instanceof VectorDataLayer) {
                productTree.deregisterActiveProductNodes(((VectorDataLayer) oldLayer).getVectorDataNode());
            }
            if (newLayer instanceof VectorDataLayer) {
                productTree.registerActiveProductNodes(((VectorDataLayer) newLayer).getVectorDataNode());
            }
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {
        private PropertyChangeListener selectedLayerPCL = new SelectedLayerListener();

        private ProductSceneView getView(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                return (ProductSceneView) contentPane;
            } else {
                return null;
            }
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final ProductSceneView sceneView = getView(e);
            if (sceneView != null) {
                sceneView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_LAYER, selectedLayerPCL);
                productTree.registerOpenedProductNodes(sceneView.getRasters());
                productTree.registerActiveProductNodes(sceneView.getRasters());
                registerActiveVectorDataNode(sceneView);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final ProductSceneView sceneView = getView(e);
            if (sceneView != null) {
                sceneView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_LAYER, selectedLayerPCL);
                productTree.deregisterOpenedProductNodes(sceneView.getRasters());
                productTree.deregisterActiveProductNodes(sceneView.getRasters());
                deregisterActiveVectorDataNode(sceneView);
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final ProductSceneView sceneView = getView(e);
            if (sceneView != null) {
                productTree.registerActiveProductNodes(sceneView.getRasters());
                registerActiveVectorDataNode(sceneView);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final ProductSceneView sceneView = getView(e);
            if (sceneView != null) {
                productTree.deregisterActiveProductNodes(sceneView.getRasters());
                deregisterActiveVectorDataNode(sceneView);
            }
        }
    }

}
