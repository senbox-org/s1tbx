/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.*;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The window containing all statistics.
 *
 * @author Marco Peters
 */
public abstract class AbstractStatisticsToolView extends AbstractToolView {

    private PagePanel pagePanel;
    private Product product;

    private final PagePanelPTL pagePanelPTL;
    private final PagePanelIFL pagePanelIFL;
    private final PagePanelLL pagePanelLL;
    private final SelectionChangeListener pagePanelSCL;

    protected AbstractStatisticsToolView() {
        pagePanelPTL = new PagePanelPTL();
        pagePanelIFL = new PagePanelIFL();
        pagePanelLL = new PagePanelLL();
        pagePanelSCL = new PagePanelSCL();
    }

    @Override
    public JComponent createControl() {
        pagePanel = createPagePanel();
        pagePanel.initComponents();
        setCurrentSelection();
        return pagePanel;
    }

    abstract protected PagePanel createPagePanel();

    @Override
    public void componentShown() {
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        productTree.addProductTreeListener(pagePanelPTL);
        VisatApp.getApp().addInternalFrameListener(pagePanelIFL);
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                addViewListener(view);
            }
        }
        setCurrentSelection();
        transferProductNodeListener(null, product);
    }

    @Override
    public void componentHidden() {
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        productTree.removeProductTreeListener(pagePanelPTL);
        transferProductNodeListener(product, null);
        VisatApp.getApp().removeInternalFrameListener(pagePanelIFL);
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                removeViewListener(view);
            }
        }
    }

    private void addViewListener(ProductSceneView view) {
        view.getRootLayer().addListener(pagePanelLL);
        view.getFigureEditor().addSelectionChangeListener(pagePanelSCL);
    }

    private void removeViewListener(ProductSceneView view) {
        view.getRootLayer().removeListener(pagePanelLL);
        view.getFigureEditor().removeSelectionChangeListener(pagePanelSCL);
    }

    private void transferProductNodeListener(Product oldProduct, Product newProduct) {
        if (oldProduct != newProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(pagePanel);
            }
            if (newProduct != null) {
                newProduct.addProductNodeListener(pagePanel);
            }
        }
    }

    private void updateTitle() {
        getDescriptor().setTitle(pagePanel.getTitle());
    }

    void setCurrentSelection() {

        Product product = null;
        RasterDataNode raster = null;
        VectorDataNode vectorDataNode = null;

        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        if (selectedNode != null && selectedNode.getProduct() != null) {
            product = selectedNode.getProduct();
        }
        if (selectedNode instanceof RasterDataNode) {
            raster = (RasterDataNode) selectedNode;
        } else if (selectedNode instanceof VectorDataNode) {
            vectorDataNode = (VectorDataNode) selectedNode;
        }

        selectionChanged(product, raster, vectorDataNode);
    }


    private void selectionChanged(final Product product, final RasterDataNode raster, final VectorDataNode vectorDataNode) {
        this.product = product;

        runInEDT(new Runnable() {
            @Override
            public void run() {
                pagePanel.selectionChanged(product, raster, vectorDataNode);
                updateTitle();
            }
        });
    }

    private void runInEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private class PagePanelPTL extends ProductTreeListenerAdapter {

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            RasterDataNode raster = null;
            if (productNode instanceof RasterDataNode) {
                raster = (RasterDataNode) productNode;
            }
            VectorDataNode vector = null;
            if (productNode instanceof VectorDataNode) {
                vector = (VectorDataNode) productNode;
                final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
                if (sceneView != null) {
                    raster = sceneView.getRaster();
                }
            }
            Product product = productNode.getProduct();
            if (product != null) {
                selectionChanged(product, raster, vector);
            }
        }

        @Override
        public void productRemoved(Product product) {
            selectionChanged(null, null, null);
        }
    }

    private class PagePanelIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                addViewListener(view);
                VectorDataNode vectorDataNode = getVectorDataNode(view);
                selectionChanged(view.getRaster().getProduct(), view.getRaster(), vectorDataNode);
            }
        }

        private VectorDataNode getVectorDataNode(ProductSceneView view) {
            final Layer rootLayer = view.getRootLayer();
            final Layer layer = LayerUtils.getChildLayer(rootLayer, LayerUtils.SearchMode.DEEP,
                                                         VectorDataLayerFilterFactory.createGeometryFilter());
            VectorDataNode vectorDataNode = null;
            if (layer instanceof VectorDataLayer) {
                VectorDataLayer vdl = (VectorDataLayer) layer;
                vectorDataNode = vdl.getVectorDataNode();
            }
            return vectorDataNode;
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                removeViewListener(view);
            }
        }
    }

    private class PagePanelLL extends AbstractLayerListener {

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            pagePanel.handleLayerContentChanged();
        }
    }

    private class PagePanelSCL implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            pagePanel.handleLayerContentChanged();
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            pagePanel.handleLayerContentChanged();
        }
    }
}
