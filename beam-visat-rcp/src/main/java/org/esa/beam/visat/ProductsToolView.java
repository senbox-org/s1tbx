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

package org.esa.beam.visat;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.TreeSelectionContext;
import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.framework.ui.product.tree.ProductTreeModel;
import org.esa.beam.framework.ui.product.tree.AbstractTN;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.actions.ShowMetadataViewAction;
import org.esa.beam.visat.actions.ShowPlacemarkViewAction;
import org.esa.beam.visat.internal.RasterDataNodeDeleter;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.TreePath;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

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
        productTree.setModel(new ProductTreeModel(visatApp.getProductManager()));
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
        Layer layer = LayerUtils.getChildLayer(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, layerFilter);
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
            final JInternalFrame frame = visatApp.findInternalFrame(vectorDataNode);
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException ignored) {
                    // ok
                }
                return;
            }
            if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand(ShowPlacemarkViewAction.ID);
                command.execute(vectorDataNode);
            }
        }

        @Override
        public void metadataElementSelected(final MetadataElement metadataElement, final int clickCount) {
            setSelectedProductNode(metadataElement);
            final JInternalFrame frame = visatApp.findInternalFrame(metadataElement);
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException ignored) {
                    // ok
                }
                return;
            }
            if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand(ShowMetadataViewAction.ID);
                command.execute(metadataElement);
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
                } catch (PropertyVetoException ignored) {
                    // ok
                }
            } else if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand("showImageView");
                command.execute(clickCount);
            }
        }


        private void setSelectedProductNode(ProductNode product) {
            visatApp.setSelectedProductNode(product);
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
            return selectedObject instanceof VectorDataNode;
        }

        private boolean isDeletableRasterData(Object selectedObject) {
            return selectedObject instanceof Band || selectedObject instanceof TiePointGrid;
        }

        @Override
        public void deleteSelection() {
            Object selectedObject = getSelectedObject();
            if (isDeletableRasterData(selectedObject)) {
                RasterDataNodeDeleter.deleteRasterDataNode((RasterDataNode) selectedObject);
            } else if (isDeletableVectorData(selectedObject)) {
                RasterDataNodeDeleter.deleteVectorDataNode((VectorDataNode) selectedObject);
            }
        }
        
        private Object getSelectedObject() {
            TreePath treePath = (TreePath) getSelection().getSelectedValue();
            return ((AbstractTN) treePath.getLastPathComponent()).getContent();
        }
    }

    private class ProductManagerL implements ProductManager.Listener {
        @Override
        public void productAdded(final ProductManager.Event event) {
            visatApp.getApplicationPage().showToolView(ID);
        }

        @Override
        public void productRemoved(final ProductManager.Event event) {
            final Product product = event.getProduct();
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
