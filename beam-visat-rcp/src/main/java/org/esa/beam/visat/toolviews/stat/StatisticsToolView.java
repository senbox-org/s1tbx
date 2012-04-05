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
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Rectangle2D;

/**
 * The window containing all statistics.
 *
 * @author Marco Peters
 */
public class StatisticsToolView extends AbstractToolView {

    public static final String ID = StatisticsToolView.class.getName();

    public static final int INFORMATION_TAB_INDEX = 0;
    public static final int GEOCODING_TAB_INDEX = 1;
    public static final int STATISTICS_TAB_INDEX = 2;
    public static final int HISTOGRAM_TAB_INDEX = 3;
    public static final int DENSITYPLOT_TAB_INDEX = 4;
    public static final int SCATTERPLOT_TAB_INDEX = 5;
    public static final int PROFILEPLOT_TAB_INDEX = 6;
    public static final int COORDLIST_TAB_INDEX = 7;

    private static final String[] helpIDs = {
            "informationDialog",
            "geoCodingInfoDialog",
            "statisticsDialog",
            "histogramDialog",
            "densityplotDialog",
            "scatterplotDialog",
            "profilePlotDialog",
            "coordinateListDialog"
    };

    private int currTabIndex;

    private JTabbedPane tabbedPane;
    private PagePanel[] pagePanels;
    private Product product;

    private final PagePanelPTL pagePanelPTL;
    private final PagePanelIFL pagePanelIFL;
    private final PagePanelLL pagePanelLL;
    private SelectionChangeListener pagePanelSCL;

    public StatisticsToolView() {
        pagePanelPTL = new PagePanelPTL();
        pagePanelIFL = new PagePanelIFL();
        pagePanelLL = new PagePanelLL();
        pagePanelSCL = new PagePanelSCL();
    }

    public void show(final int tabIndex) {
        VisatApp.getApp().getApplicationPage().showToolView(StatisticsToolView.ID);
        if (!isValidTabIndex(tabIndex)) {
            throw new IllegalArgumentException("illegal tab-index");
        }
        currTabIndex = tabIndex;
        tabbedPane.setSelectedIndex(tabIndex);
        updateUIState();
    }

    @Override
    public JComponent createControl() {

        tabbedPane = new JTabbedPane();
        final InformationPanel informationPanel = new InformationPanel(this, helpIDs[0]);
        final GeoCodingPanel codingPanel = new GeoCodingPanel(this, helpIDs[1]);
        final StatisticsPanel statisticsPanel = new StatisticsPanel(this, helpIDs[2]);
        final HistogramPanel histogramPanel = new HistogramPanel(this, helpIDs[3]);
        final DensityPlotPanel densityPlotPanel = new DensityPlotPanel(this, helpIDs[4]);
        final ScatterPlotPanel scatterPlotPanel = new ScatterPlotPanel(this, helpIDs[5]);
        final ProfilePlotPanel profilePlotPanel = new ProfilePlotPanel(this, helpIDs[6]);
        final CoordListPanel coordListPanel = new CoordListPanel(this, helpIDs[7]);
        pagePanels = new PagePanel[]{
                informationPanel, codingPanel, statisticsPanel, histogramPanel,
                densityPlotPanel, scatterPlotPanel, profilePlotPanel, coordListPanel
        };
        tabbedPane.add("Information", informationPanel); /*I18N*/
        tabbedPane.add("Geo-Coding", codingPanel);/*I18N*/
        tabbedPane.add("Statistics", statisticsPanel); /*I18N*/
        tabbedPane.add("Histogram", histogramPanel);  /*I18N*/
        tabbedPane.add("Density Plot", densityPlotPanel); /*I18N*/
        tabbedPane.add("Scatter Plot", scatterPlotPanel); /*I18N*/
        tabbedPane.add("Profile Plot", profilePlotPanel);  /*I18N*/
        tabbedPane.add("Coordinate List", coordListPanel);  /*I18N*/

        tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedIndex() != currTabIndex) {
                    currTabIndex = tabbedPane.getSelectedIndex();
                    updateUIState();
                }
            }
        });
        tabbedPane.setSelectedIndex(0);
        updateUIState();
        return tabbedPane;
    }

    @Override
    public void componentShown() {
        updateCurrentSelection();
        updateUI();
    }

    @Override
    public void componentOpened() {
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        productTree.addProductTreeListener(pagePanelPTL);
        transferProductNodeListener(product, null);
        VisatApp.getApp().addInternalFrameListener(pagePanelIFL);
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                addViewListener(view);
            }
        }
        updateCurrentSelection();
        transferProductNodeListener(null, product);
        updateUI();
    }

    private void addViewListener(ProductSceneView view) {
        view.getRootLayer().addListener(pagePanelLL);
        view.getFigureEditor().addSelectionChangeListener(pagePanelSCL);
    }

    private void removeViewListener(ProductSceneView view) {
        view.getRootLayer().removeListener(pagePanelLL);
        view.getFigureEditor().removeSelectionChangeListener(pagePanelSCL);
    }

    private void updateUI() {
        for (PagePanel pagePanel : pagePanels) {
            pagePanel.updateUI();
        }
    }

    private void updateCurrentSelection() {
        for (PagePanel pagePanel : pagePanels) {
            pagePanel.updateCurrentSelection();
        }
    }

    private void transferProductNodeListener(Product oldProduct, Product newProduct) {
        if (oldProduct != newProduct) {
            for (PagePanel pagePanel : pagePanels) {
                if (oldProduct != null) {
                    oldProduct.removeProductNodeListener(pagePanel);
                }
                if (newProduct != null) {
                    newProduct.addProductNodeListener(pagePanel);
                }
            }
        }
    }


    @Override
    public void componentClosed() {
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

    private static boolean isValidTabIndex(final int tabIndex) {
        return tabIndex == INFORMATION_TAB_INDEX ||
               tabIndex == STATISTICS_TAB_INDEX ||
               tabIndex == HISTOGRAM_TAB_INDEX ||
               tabIndex == DENSITYPLOT_TAB_INDEX ||
               tabIndex == SCATTERPLOT_TAB_INDEX ||
               tabIndex == PROFILEPLOT_TAB_INDEX ||
               tabIndex == COORDLIST_TAB_INDEX ||
               tabIndex == GEOCODING_TAB_INDEX;
    }

    private void updateHelpBroker() {
        Debug.assertTrue(currTabIndex >= 0 && currTabIndex < helpIDs.length);
        setCurrentHelpID(helpIDs[currTabIndex]);
    }

    private void setCurrentHelpID(String helpID) {
        HelpSys.enableHelpKey(getPaneControl(), helpID);
        HelpSys.enableHelpKey(tabbedPane, helpID);
        HelpSys.getHelpBroker().setCurrentID(helpID);
    }

    private void updateUIState() {
        if (tabbedPane != null) {
            final Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof PagePanel) {
                final PagePanel pagePanel = (PagePanel) selectedComponent;
                pagePanel.getParentDialog().getDescriptor().setTitle(pagePanel.getTitle());
            } else {
                setTitle("");
            }
        }
        updateHelpBroker();
    }

    private void selectionChanged(Product product, RasterDataNode raster, VectorDataNode vectorDataNode) {
        this.product = product;
        if (pagePanels == null) {
            return;
        }
        final PagePanel[] panels = pagePanels;
        for (PagePanel panel : panels) {
            panel.selectionChanged(product, raster, vectorDataNode);
        }
        updateUIState();
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
            selectionChanged(product, raster, vector);
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
                selectionChanged(null, null, null);
            }
        }

    }

    private class PagePanelLL extends AbstractLayerListener {

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            final PagePanel[] panels = StatisticsToolView.this.pagePanels;
            for (PagePanel panel : panels) {
                panel.handleLayerContentChanged();
            }
        }
    }

    private class PagePanelSCL implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            final PagePanel[] panels = StatisticsToolView.this.pagePanels;
            for (PagePanel panel : panels) {
                panel.handleLayerContentChanged();
            }

        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            final PagePanel[] panels = StatisticsToolView.this.pagePanels;
            for (PagePanel panel : panels) {
                panel.handleLayerContentChanged();
            }

        }
    }
}
