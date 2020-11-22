/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions.GraphExportImageAction;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions.TimeSeriesExportAction;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions.TimeSeriesFilterAction;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions.TimeSeriesSettingsAction;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Debug;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.diagram.DefaultDiagramGraphStyle;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

@TopComponent.Description(
        preferredID = "TimeSeriesToolView",
        iconBase = "org/esa/s1tbx/analysis/icons/timeseries.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows/Radar"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TimeSeriesToolView_Name",
        preferredID = "TimeSeriesToolView"
)
@NbBundle.Messages({
        "CTL_TimeSeriesToolView_Name=Time Series",
        "CTL_TimeSeriesToolViewDescription=Shows historic values of a pixel over time",
})
/**
 * A window which displays product spectra.
 */

public class TimeSeriesToolView extends ToolTopComponent {

    private static final String MSG_NO_PRODUCTS = "Add a product stack with the Settings button on the right.";
    private static final String MSG_NO_BANDS = "Filter bands with the Filter button on the right.";
    private static final String MSG_NO_IMAGE_VIEW = "Open a band image for viewing.";

    private final Map<Product, TimeSeriesDiagram> productToDiagramMap;
    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final CursorPPL ppl;

    private TimeSeriesCanvas diagramCanvas;
    private AbstractButton filterButton;
    private AbstractButton showForCursorButton;
    private AbstractButton showForSelectedPinsButton;
    private AbstractButton showForAllPinsButton;
    private AbstractButton showVectorAverageButton;
    private AbstractButton showAveragePinSpectrumButton;
    private AbstractButton exportTextButton;
    private AbstractButton exportImageButton;

    private ProductSceneView currentView;
    private Product currentProduct;
    private boolean isShown = false;

    private ProductNode oldNode = null;

    private final TimeSeriesSettings settings = new TimeSeriesSettings();

    public TimeSeriesToolView() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_TimeSeriesToolView_Name());
        setToolTipText(Bundle.CTL_TimeSeriesToolViewDescription());
        add(createPanel(), BorderLayout.CENTER);

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());
        snapApp.getSelectionSupport(ProductNode.class).addHandler((oldValue, newValue) -> {
            if (newValue != null && newValue != oldNode) {

                oldNode = newValue;
            }
        });

        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        productToDiagramMap = new HashMap<>(4);
        ppl = new CursorPPL();
    }

    @Override
    protected void componentOpened() {
        isShown = true;
        if (currentView == null) {
            final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
            if (view != null) {
                handleViewActivated(view);
            }
        }
    }

    @Override
    protected void componentClosed() {
        isShown = false;
    }

    @Override
    protected void productSceneViewSelected(ProductSceneView view) {
        handleViewActivated(view);
    }

    @Override
    protected void productSceneViewDeselected(ProductSceneView view) {
        handleViewDeactivated(view);
    }

    private void setCurrentView(final ProductSceneView view) {
        ProductSceneView oldView = currentView;
        currentView = view;
        if (oldView != currentView) {
            if (oldView != null) {
                oldView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
            }
            if (currentView != null) {
                currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
                setCurrentProduct(currentView.getProduct());
            } else {
                setCurrentProduct(null);
            }
            updateUIState();
        }
    }

    public Product getCurrentProduct() {
        return currentProduct;
    }

    private void setCurrentProduct(final Product product) {
        Product oldProduct = currentProduct;
        currentProduct = product;
        if (currentProduct != oldProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(productNodeHandler);
            }
            if (currentProduct != null) {
                currentProduct.addProductNodeListener(productNodeHandler);
                final TimeSeriesDiagram diagram = getDiagram();
                if (diagram != null) {
                    diagramCanvas.setDiagram(diagram);
                } else {
                    recreateDiagram();
                }
            }
            if (currentProduct == null) {
                diagramCanvas.setDiagram(null);
                diagramCanvas.setMessageText(MSG_NO_PRODUCTS);
            } else {
                diagramCanvas.setMessageText(null);
            }
            updateUIState();
        }
    }

    private void updateUIState() {
        boolean hasView = currentView != null;
        boolean hasProduct = currentProduct != null;
        boolean hasSelectedPins = hasView && currentView.getSelectedPins().length > 0;
        boolean hasPins = hasProduct && currentProduct.getPinGroup().getNodeCount() > 0;
        boolean hasDiagram = diagramCanvas.getDiagram() != null;
        filterButton.setEnabled(settings.hasProducts());
        showForCursorButton.setEnabled(hasView);
        showForSelectedPinsButton.setEnabled(hasSelectedPins);
        showForAllPinsButton.setEnabled(hasPins);
        showVectorAverageButton.setEnabled(hasDiagram);
        showAveragePinSpectrumButton.setEnabled(hasPins);
        diagramCanvas.setEnabled(hasProduct);
        exportTextButton.setEnabled(hasProduct);
        exportImageButton.setEnabled(hasProduct);

        setCanvasHelpMessage();

        if (diagramCanvas.getDiagram() != null) {
            diagramCanvas.getDiagram().setDrawGrid(settings.isShowingGrid());
        }
    }

    private boolean setCanvasHelpMessage() {
        if(!settings.hasProducts()) {
            diagramCanvas.setMessageText(MSG_NO_PRODUCTS);
            return true;
        } else if(settings.getSelectedBands() == null) {
            diagramCanvas.setMessageText(MSG_NO_BANDS);
            return true;
        } else if(currentProduct == null) {
            diagramCanvas.setMessageText(MSG_NO_IMAGE_VIEW);
            return true;
        }
        return false;
    }

    private void updateDiagram(final ImageLayer imageLayer, final int pixelX, final int pixelY, final int level) {
        if(setCanvasHelpMessage()) {
            return;
        }
        final TimeSeriesDiagram diagram = getDiagram();
        final String[] selectedBands = settings.getSelectedBands();
        if (diagram != null && selectedBands != null && selectedBands.length > 0) {
            final boolean isShowingVectors = isShowingVectorAverage();
            final String[] selectedVectors = settings.getSelectedVectorNames();
            if(isShowingVectors && (selectedVectors == null || selectedVectors.length == 0)) {
                // select default vector if none selected
                setDefaultVector();
            }

            diagram.updateDiagram(imageLayer, pixelX, pixelY, level);
        }
    }

    private void setDefaultVector() {
        if(currentProduct != null && currentProduct.getVectorDataGroup().contains("geometry")) {
            settings.setSelectedVectors(new String[] {"geometry"});
        }
    }

    private TimeSeriesTimes getProductTimes() {

        final boolean isCoreg = StackUtils.isCoregisteredStack(currentProduct);
        if (isCoreg) {
            final ProductData.UTC[] coregTimes = StackUtils.getProductTimes(currentProduct);
            return new TimeSeriesTimes(coregTimes);
        } else {
            final List<Product> products = new ArrayList<>(100);
            // get all times for all lists
            for (GraphData data : settings.getGraphDataList()) {
                products.addAll(Arrays.asList(data.getProducts()));
            }

            final ArrayList<ProductData.UTC> utcList = new ArrayList<>(products.size());
            for (Product prod : products) {
                utcList.add(prod.getStartTime());
            }
            return new TimeSeriesTimes(utcList.toArray(new ProductData.UTC[0]));
        }
    }

    private Band[] findBandAcrossProducts(final String selectedBandName, final Product[] products) {
        final boolean isCoreg = StackUtils.isCoregisteredStack(currentProduct);
        final ArrayList<Band> bands;
        if (isCoreg) {
            bands = new ArrayList<>();
            final String bandName = TimeSeriesFilterAction.getCoregBandName(selectedBandName);
            // find all bands starting with the same name
            for (Band band : currentProduct.getBands()) {
                if (band.getName().startsWith(bandName)) {
                    bands.add(band);
                }
            }
        } else {
            bands = new ArrayList<>(products.length);
            for (Product prod : products) {
                // find all bands across products with same name as selected product bands
                final Band prodBand = prod.getBand(selectedBandName);
                if (prodBand != null) {
                    bands.add(prodBand);
                }
            }
        }
        return bands.toArray(new Band[0]);
    }

    public JComponent createPanel() {

        final AbstractButton settingsButton = DialogUtils.createButton("Settings", "Settings",
                                                                       UIUtils.loadImageIcon("icons/Properties24.gif"), null, DialogUtils.ButtonStyle.Icon);
        settingsButton.addActionListener(new TimeSeriesSettingsAction(this, settings));

        filterButton = DialogUtils.createButton("filterButton", "Filter bands and vectors",
                                                UIUtils.loadImageIcon("icons/Filter24.gif"), null, DialogUtils.ButtonStyle.Icon);
        filterButton.setEnabled(false);
        filterButton.addActionListener(new TimeSeriesFilterAction(this, settings));

        showForCursorButton = DialogUtils.createIconButton("showForCursorButton", "Show at cursor position",
                                                           UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showForCursorButton.addActionListener(e -> recreateDiagram());
        showForCursorButton.setSelected(false);

        showForSelectedPinsButton = DialogUtils.createIconButton("showForSelectedPinsButton", "Show for selected pins",
                                                                 UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showForSelectedPinsButton.addActionListener(e -> {
            if (isShowingForAllPins()) {
                showForAllPinsButton.setSelected(false);
            }
            recreateDiagram();
        });

        showForAllPinsButton = DialogUtils.createIconButton("showForAllPinsButton", "Show for all pins",
                                                            UIUtils.loadImageIcon("icons/PinSpectra24.gif"),
                                                            true);
        showForAllPinsButton.addActionListener(e -> {
            if (isShowingForSelectedPins()) {
                showForSelectedPinsButton.setSelected(false);
            }
            recreateDiagram();
        });

        showAveragePinSpectrumButton = DialogUtils.createIconButton("showAveragePinSpectrumButton", "Show average of all pins",
                                                                    UIUtils.loadImageIcon("icons/AverageSpectrum24.gif"), true);
        showAveragePinSpectrumButton.addActionListener(e -> {
            // todo - implement
            JOptionPane.showMessageDialog(null, "Not implemented");
        });

        showVectorAverageButton = DialogUtils.createIconButton("showVectorAverageButton", "Show averaged ROI",
                                                               UIUtils.loadImageIcon("icons/VectorDataNode24.gif"), true);
        showVectorAverageButton.addActionListener(e -> {
            recreateDiagram();
        });

        exportTextButton = DialogUtils.createIconButton("exportTextButton", "Export graph to text file.",
                                                        UIUtils.loadImageIcon("icons/ExportTable.gif"), false);
        exportTextButton.addActionListener(new TimeSeriesExportAction(this));

        exportImageButton = DialogUtils.createIconButton("exportImageButton", "Export graph to image file.",
                                                         UIUtils.loadImageIcon("icons/Export24.gif"), false);
        exportImageButton.addActionListener(e -> exportImage());

        final AbstractButton helpButton = DialogUtils.createIconButton("helpButton", "Help",
                                                                       UIUtils.loadImageIcon("icons/Help24.gif"), false);

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPane.add(settingsButton, gbc);
        gbc.gridy++;
        buttonPane.add(filterButton, gbc);
        gbc.gridy++;
        buttonPane.add(showForCursorButton, gbc);
        gbc.gridy++;
        buttonPane.add(showForSelectedPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showForAllPinsButton, gbc);
        //gbc.gridy++;
        //buttonPane.add(showAveragePinSpectrumButton, gbc);
        gbc.gridy++;
        buttonPane.add(showVectorAverageButton, gbc);
        gbc.gridy++;
        buttonPane.add(exportTextButton, gbc);
        gbc.gridy++;
        buttonPane.add(exportImageButton, gbc);

        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPane.add(helpButton, gbc);

        diagramCanvas = new TimeSeriesCanvas(settings);
        diagramCanvas.setPreferredSize(new Dimension(300, 200));
        diagramCanvas.setMessageText(MSG_NO_PRODUCTS);
        diagramCanvas.setBackground(Color.white);
        diagramCanvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, diagramCanvas);
        mainPane.add(BorderLayout.EAST, buttonPane);
        mainPane.setPreferredSize(new Dimension(320, 200));

        final ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null) {
            handleViewActivated(view);
        } else {
            setCurrentView(view);
        }
        updateUIState();
        return mainPane;
    }

    private void exportImage() {
        GraphExportImageAction action = new GraphExportImageAction(getDiagram());
        action.exportImage();
    }

    public void refresh() {
        recreateDiagram();
        updateUIState();
    }

    public TimeSeriesDiagram getDiagram() {
        Debug.assertNotNull(currentProduct);
        return productToDiagramMap.get(currentProduct);
    }

    private void setDiagram(final TimeSeriesDiagram newDiagram) {
        Debug.assertNotNull(currentProduct);
        TimeSeriesDiagram oldDiagram;
        if (newDiagram != null) {
            oldDiagram = productToDiagramMap.put(currentProduct, newDiagram);
        } else {
            oldDiagram = productToDiagramMap.remove(currentProduct);
        }
        diagramCanvas.setDiagram(newDiagram);
        if (oldDiagram != null && oldDiagram != newDiagram) {
            oldDiagram.dispose();
        }
    }

    private boolean isShowingCursor() {
        return showForCursorButton.isSelected();
    }

    private boolean isShowingPinSpectra() {
        return isShowingForSelectedPins() || isShowingForAllPins();
    }

    private boolean isShowingForAllPins() {
        return showForAllPinsButton.isSelected();
    }

    private boolean isShowingVectorAverage() {
        return showVectorAverageButton.isSelected();
    }

    public TimeSeriesSettings getSettings() {
        return settings;
    }

    private void recreateDiagram() {
        try {
            //todo time axis from 0 to 1.0 and pass in earliest time and latest time
            //todo pass product start times and convert to time between 0 and 1.0
            //todo
            if (currentView == null)
                return;
            final Product[] defaultProductList = settings.getGraphDataList().get(0).getProducts();
            if (defaultProductList == null || defaultProductList.length == 0)
                return;

            final String[] selectedBands = settings.getSelectedBands();
            if(selectedBands == null)
                return;

            final TimeSeriesDiagram diagram = new TimeSeriesDiagram(currentProduct);

            final TimeSeriesTimes times = getProductTimes();

            for (String selectedBandName : selectedBands) {
                if (isShowingForAllPins() || isShowingForSelectedPins()) {
                    Placemark[] pins = null;
                    if (isShowingForSelectedPins()) {
                        pins = currentView.getSelectedPins();
                    } else if (isShowingForAllPins()) {
                        final ProductNodeGroup<Placemark> pinGroup = currentProduct.getPinGroup();
                        pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
                    }
                    if(pins != null) {
                        for (Placemark pin : pins) {
                            for (GraphData graphData : settings.getGraphDataList()) {
                                if (graphData.getProducts() != null) {
                                    final Band[] bands = findBandAcrossProducts(selectedBandName, graphData.getProducts());
                                    if (bands != null && bands.length > 0) {
                                        final TimeSeriesGraph graph = diagram.addPlacemarkGraph(pin, graphData);
                                        DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
                                        style.setOutlineColor(settings.getPinColor(pin.getName()));
                                        style.setOutlineStroke(settings.getBandStroke(selectedBandName));
                                        graph.setBands(times, bands);
                                    }
                                }
                            }
                        }
                    }
                }

                if (isShowingVectorAverage()) {
                    final ProductNodeGroup<VectorDataNode> vectorNodeGroup = currentProduct.getVectorDataGroup();
                    if (vectorNodeGroup != null) {
                        final String[] vectorNames = settings.getSelectedVectorNames();
                        for (String name : vectorNames) {
                            for (GraphData graphData : settings.getGraphDataList()) {
                                if (graphData.getProducts() != null) {
                                    final Band[] bands = findBandAcrossProducts(selectedBandName, graphData.getProducts());
                                    if (bands != null && bands.length > 0) {
                                        final VectorDataNode vectorNode = vectorNodeGroup.get(name);
                                        final TimeSeriesGraph graph = diagram.addVectorGraph(
                                                vectorNode, graphData, settings.getVectorStatistic());
                                        DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
                                        style.setOutlineColor(settings.getVectorColor(name));
                                        style.setOutlineStroke(settings.getBandStroke(selectedBandName));
                                        graph.setBands(times, bands);
                                    }
                                }
                            }
                        }
                    }
                }

                if (isShowingCursor()) {
                    for (GraphData graphData : settings.getGraphDataList()) {
                        if (graphData.getProducts() != null) {
                            final Band[] bandsAcrossProducts = findBandAcrossProducts(selectedBandName, graphData.getProducts());
                            if (bandsAcrossProducts != null && bandsAcrossProducts.length > 0) {
                                final TimeSeriesGraph graph = diagram.addCursorGraph(graphData);
                                DefaultDiagramGraphStyle style = (DefaultDiagramGraphStyle) graph.getStyle();
                                style.setOutlineColor(Color.BLACK);
                                BasicStroke stroke = settings.getBandStroke(selectedBandName);
                                style.setOutlineStroke(stroke);
                                graph.setBands(times, bandsAcrossProducts);
                            }
                        }
                    }
                }
            }
            diagram.initAxis(times, findBandAcrossProducts(selectedBands[0], defaultProductList));

            setDiagram(diagram);
            updateUIState();
        } catch (Exception e) {
            System.out.println("Failed to create time series graphs " + e.getMessage());
        }
    }

    private boolean isShowingForSelectedPins() {
        return showForSelectedPinsButton.isSelected();
    }

    private void handleViewActivated(final ProductSceneView view) {
        if (!isShown) return;
        view.addPixelPositionListener(ppl);
        setCurrentView(view);
    }

    private void handleViewDeactivated(final ProductSceneView view) {
        if (!isShown) return;
        view.removePixelPositionListener(ppl);
        setCurrentView(null);
    }

    /////////////////////////////////////////////////////////////////////////
    // View change handling

    /////////////////////////////////////////////////////////////////////////
    // Pixel position change handling

    private class CursorPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    int pixelX,
                                    int pixelY,
                                    int currentLevel,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            if (!isShown) return;

            diagramCanvas.setMessageText(null);
            if (pixelPosValid) {
                //getDiagram().addCursorGraph();
                updateDiagram(imageLayer, pixelX, pixelY, currentLevel);
            }
            if (e.isShiftDown()) {
                final TimeSeriesDiagram diagram = getDiagram();
                if (diagram != null) {
                    diagram.adjustAxes(true);
                }
            }
        }

        @Override
        public void pixelPosNotAvailable() {

            if (isActive()) {
                //getDiagram().removeCursorGraph();
                diagramCanvas.repaint();
            }
        }

        private boolean isActive() {
            return isVisible() && isShowingCursor() && getDiagram() != null;
        }

    }

    /////////////////////////////////////////////////////////////////////////
    // Product change handling

    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                final String propertyName = event.getPropertyName();
                if (propertyName.equals(DataNode.PROPERTY_NAME_UNIT)
                        || propertyName.equals(Band.PROPERTY_NAME_SPECTRAL_WAVELENGTH)) {
                    recreateDiagram();
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateDiagram();
                }
            }
            updateUIState();
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                recreateDiagram();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateDiagram();
                }
            }
            updateUIState();
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                recreateDiagram();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateDiagram();
                }
            }
            updateUIState();
        }

        private boolean isActive() {
            return isVisible() && currentProduct != null;
        }
    }

    public class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();

        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            final Product product = event.getProduct();
            if (currentProduct == product) {
                setDiagram(null);
                setCurrentView(null);
                setCurrentProduct(null);
                settings.setSelectedBands(null);
            }
        }
    }

    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            recreateDiagram();
        }

    }
}
