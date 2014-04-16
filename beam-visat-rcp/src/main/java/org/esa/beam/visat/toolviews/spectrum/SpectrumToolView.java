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
package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.spectrum.DisplayableSpectrum;
import org.esa.beam.framework.ui.product.spectrum.SpectrumBand;
import org.esa.beam.framework.ui.product.spectrum.SpectrumChooser;
import org.esa.beam.framework.ui.product.spectrum.SpectrumShapeProvider;
import org.esa.beam.framework.ui.product.spectrum.SpectrumStrokeProvider;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.nav.CursorSynchronizer;
import org.esa.beam.visat.toolviews.placemark.PlacemarkUtils;
import org.esa.beam.visat.toolviews.stat.XYPlotMarker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A window which displays product allSpectra.
 */
public class SpectrumToolView extends AbstractToolView {

    public static final String ID = SpectrumToolView.class.getName();
    public static final String CHART_TITLE = "Spectrum View";

    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";

    private final Map<Product, DisplayableSpectrum[]> productToAllSpectraMap;
    private final Map<Product, List<SpectrumBand>> productToBandsMap;

    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final PixelPositionListener pixelPositionListener;

    private AbstractButton filterButton;
    private AbstractButton showSpectrumForCursorButton;
    private AbstractButton showSpectraForSelectedPinsButton;
    private AbstractButton showSpectraForAllPinsButton;
    private AbstractButton showGridButton;
// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//    private AbstractButton showAveragePinSpectrumButton;
    //    private AbstractButton showGraphPointsButton;

    private String titleBase;
    private boolean tipShown;
    private ProductSceneView currentView;
    private Product currentProduct;
    private ChartPanel chartPanel;
    private ChartHandler chartHandler;
    private CursorSynchronizer cursorSynchronizer;

    private boolean domainAxisAdjustmentIsFrozen;
    private boolean rangeAxisAdjustmentIsFrozen;
    private boolean isCodeInducedAxisChange;
    private boolean isUserInducedAutomaticAdjustmentChosen;

    public SpectrumToolView() {
        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        productToAllSpectraMap = new HashMap<Product, DisplayableSpectrum[]>();
        productToBandsMap = new HashMap<Product, List<SpectrumBand>>();
        pixelPositionListener = new CursorSpectrumPixelPositionListener(this);
    }

    private ProductSceneView getCurrentView() {
        return currentView;
    }

    private void setCurrentView(ProductSceneView view) {
        ProductSceneView oldView = currentView;
        currentView = view;
        if (oldView != currentView) {
            if (oldView != null) {
                oldView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
            }
            if (currentView != null) {
                currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionChangeListener);
                setCurrentProduct(currentView.getProduct());
            }
            updateUIState();
        }
    }

    private Product getCurrentProduct() {
        return currentProduct;
    }

    private void setCurrentProduct(Product product) {
        Product oldProduct = currentProduct;
        currentProduct = product;
        if (currentProduct != oldProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(productNodeHandler);
            }
            if (currentProduct != null) {
                currentProduct.addProductNodeListener(productNodeHandler);
                if (!productToAllSpectraMap.containsKey(currentProduct)) {
                    setUpSpectra();
                }
                recreateChart();
            }
            if (currentProduct == null) {
                chartHandler.setEmptyPlot();
            }
            updateUIState();
            updateTitle();
        }
    }

    private void updateTitle() {
        if (currentProduct != null) {
            setTitle(titleBase + " - " + currentView.getProduct().getProductRefString());
        } else {
            setTitle(titleBase);
        }
    }

    private void updateUIState() {
        boolean hasView = getCurrentView() != null;
        boolean hasProduct = getCurrentProduct() != null;
        boolean hasSelectedPins = hasView && getCurrentView().getSelectedPins().length > 0;
        boolean hasPins = hasProduct && getCurrentProduct().getPinGroup().getNodeCount() > 0;
        boolean chartHandlerHasDiagram = chartHandler.hasDiagram();
        filterButton.setEnabled(hasProduct);
        showSpectrumForCursorButton.setEnabled(hasView);
        showSpectraForSelectedPinsButton.setEnabled(hasSelectedPins);
        showSpectraForAllPinsButton.setEnabled(hasPins);
        showGridButton.setEnabled(chartHandlerHasDiagram);
// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        showAveragePinSpectrumButton.setEnabled(hasPins); // todo - hasSpectraGraphs
//        showGraphPointsButton.setEnabled(chartHandlerHasDiagram);
        chartPanel.setEnabled(hasProduct);    // todo - hasSpectraGraphs
        if (chartHandlerHasDiagram) {
            showGridButton.setSelected(true);
        }
    }

    @Override
    public void componentShown() {
        super.componentShown();

    }

    void setPrepareForUpdateMessage() {
        chartHandler.setCollectingSpectralInformationMessage();
    }

    void updateData(int pixelX, int pixelY, int level) {
        chartHandler.setPosition(pixelX, pixelY);
        chartHandler.setLevel(level);
        chartHandler.updateData();
    }

    void updateChart(boolean adjustAxes) {
        chartHandler.setAutomaticRangeAdjustments(adjustAxes);
        updateChart();
    }

    void updateChart() {
        maybeShowTip();
        chartHandler.updateChart();
        chartPanel.repaint();
    }

    private void maybeShowTip() {
        if (!tipShown) {
            final String message = "Tip: If you press the SHIFT key while moving the mouse cursor over \n" +
                    "an image, " + VisatApp.getApp().getAppName() + " adjusts the diagram axes " +
                    "to the local values at the\n" +
                    "current pixel position, if you release the SHIFT key again, then the\n" +
                    "min/max are accumulated again.";
            VisatApp.getApp().showInfoDialog("Spectrum Tip", message, SUPPRESS_MESSAGE_KEY);
            tipShown = true;
        }
    }

    private SpectrumBand[] getAvailableSpectralBands() {
        Debug.assertNotNull(currentProduct);
        if (!productToBandsMap.containsKey(currentProduct)) {
            productToBandsMap.put(currentProduct, new ArrayList<SpectrumBand>());
        }
        List<SpectrumBand> spectrumBands = productToBandsMap.get(currentProduct);
        Band[] bands = currentProduct.getBands();
        for (Band band : bands) {
            if (isSpectralBand(band)) {
                if (!band.isFlagBand()) {
                    boolean isAlreadyIncluded = false;
                    for (SpectrumBand spectrumBand : spectrumBands) {
                        if (spectrumBand.getOriginalBand() == band) {
                            isAlreadyIncluded = true;
                            break;
                        }
                    }
                    if (!isAlreadyIncluded) {
                        spectrumBands.add(new SpectrumBand(band, true));
                    }
                }
            }
        }
        return spectrumBands.toArray(new SpectrumBand[spectrumBands.size()]);
    }

    private boolean isSpectralBand(Band band) {
        return band.getSpectralWavelength() > 0.0;
    }

    @Override
    public JComponent createControl() {
        final JFreeChart chart = ChartFactory.createXYLineChart(CHART_TITLE, "Wavelength (nm)", "", null, PlotOrientation.VERTICAL, true, true, false);
        chart.getXYPlot().getRangeAxis().addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                if (!isCodeInducedAxisChange) {
                    rangeAxisAdjustmentIsFrozen = !((ValueAxis) axisChangeEvent.getAxis()).isAutoRange();
                }
            }
        });
        chart.getXYPlot().getDomainAxis().addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                if (!isCodeInducedAxisChange) {
                    domainAxisAdjustmentIsFrozen = !((ValueAxis) axisChangeEvent.getAxis()).isAutoRange();
                }
            }
        });
        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        rangeAxisAdjustmentIsFrozen = false;
        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        domainAxisAdjustmentIsFrozen = false;
        chartPanel = new ChartPanel(chart);
        chartHandler = new ChartHandler(chart);
        final XYPlotMarker plotMarker = new XYPlotMarker(chartPanel, new XYPlotMarker.Listener() {
            @Override
            public void pointSelected(XYDataset xyDataset, int seriesIndex, Point2D dataPoint) {
                if (hasDiagram()) {
                    if (cursorSynchronizer == null) {
                        cursorSynchronizer = new CursorSynchronizer(VisatApp.getApp());
                    }
                    if (!cursorSynchronizer.isEnabled()) {
                        cursorSynchronizer.setEnabled(true);
                    }
                }
            }

            @Override
            public void pointDeselected() {
                cursorSynchronizer.setEnabled(false);
            }
        });

        titleBase = getDescriptor().getTitle();
        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"), false);
        filterButton.setName("filterButton");
        filterButton.setEnabled(false);
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectSpectralBands();
                recreateChart();
            }
        });

        showSpectrumForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showSpectrumForCursorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recreateChart();
            }
        });
        showSpectrumForCursorButton.setName("showSpectrumForCursorButton");
        showSpectrumForCursorButton.setSelected(true);
        showSpectrumForCursorButton.setToolTipText("Show spectrum at cursor position.");

        showSpectraForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showSpectraForSelectedPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowingSpectraForAllPins()) {
                    showSpectraForAllPinsButton.setSelected(false);
                } else if (!isShowingSpectraForSelectedPins()) {
                    plotMarker.setInvisible();
                }
                recreateChart();
            }
        });
        showSpectraForSelectedPinsButton.setName("showSpectraForSelectedPinsButton");
        showSpectraForSelectedPinsButton.setToolTipText("Show spectra for selected pins.");

        showSpectraForAllPinsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/PinSpectra24.gif"),
                                                                     true);
        showSpectraForAllPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowingSpectraForSelectedPins()) {
                    showSpectraForSelectedPinsButton.setSelected(false);
                } else if (!isShowingSpectraForAllPins()) {
                    plotMarker.setInvisible();
                }
                recreateChart();
            }
        });
        showSpectraForAllPinsButton.setName("showSpectraForAllPinsButton");
        showSpectraForAllPinsButton.setToolTipText("Show spectra for all pins.");

// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        showAveragePinSpectrumButton = ToolButtonFactory.createButton(
//                UIUtils.loadImageIcon("icons/AverageSpectrum24.gif"), true);
//        showAveragePinSpectrumButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                // todo - implement
//            }
//        });
//        showAveragePinSpectrumButton.setName("showAveragePinSpectrumButton");
//        showAveragePinSpectrumButton.setToolTipText("Show average spectrum of all pin spectra.");

        showGridButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SpectrumGrid24.gif"), true);
        showGridButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartHandler.setGridVisible(showGridButton.isSelected());
            }
        });
        showGridButton.setName("showGridButton");
        showGridButton.setToolTipText("Show diagram grid.");

// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        showGraphPointsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/GraphPoints24.gif"), true);
//        showGraphPointsButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                // todo - implement
//                JOptionPane.showMessageDialog(null, "Not implemented");
//            }
//        });
//        showGraphPointsButton.setName("showGraphPointsButton");
//        showGraphPointsButton.setToolTipText("Show graph points grid.");

        AbstractButton exportSpectraButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"),
                                                                            false);
        exportSpectraButton.addActionListener(new SpectraExportAction(this));
        exportSpectraButton.setToolTipText("Export spectra to text file.");
        exportSpectraButton.setName("exportSpectraButton");

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help."); /*I18N*/

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPane.add(filterButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectrumForCursorButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectraForSelectedPinsButton, gbc);
        gbc.gridy++;
        buttonPane.add(showSpectraForAllPinsButton, gbc);
        gbc.gridy++;
// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        buttonPane.add(showAveragePinSpectrumButton, gbc);
//        gbc.gridy++;
        buttonPane.add(showGridButton, gbc);
        gbc.gridy++;
// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        buttonPane.add(showGraphPointsButton, gbc);
//        gbc.gridy++;
        buttonPane.add(exportSpectraButton, gbc);

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

        chartPanel.setPreferredSize(new Dimension(300, 200));
        chartPanel.setBackground(Color.white);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        chartPanel.addChartMouseListener(plotMarker);

        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, chartPanel);
        mainPane.add(BorderLayout.EAST, buttonPane);
        mainPane.setPreferredSize(new Dimension(320, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }

        // Add an internal frame listener to VISAT so that we can update our
        // spectrum dialog with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new SpectrumIFL());


        VisatApp.getApp().getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                // ignored
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final Product product = event.getProduct();
                if (getCurrentProduct() == product) {
                    chartPanel.getChart().getXYPlot().setDataset(null);
                    setCurrentView(null);
                    setCurrentProduct(null);
                }
                if (productToAllSpectraMap.containsKey(product)) {
                    productToAllSpectraMap.remove(product);
                }
                if (productToBandsMap.containsKey(product)) {
                    productToBandsMap.remove(product);
                }
                PlacemarkGroup pinGroup = product.getPinGroup();
                for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                    chartHandler.removePinInformation(pinGroup.get(i));
                }
            }
        });


        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            handleViewActivated(view);
        } else {
            setCurrentView(view);
        }
        updateUIState();
        return mainPane;
    }

    private void selectSpectralBands() {
        final DisplayableSpectrum[] allSpectra = productToAllSpectraMap.get(getCurrentProduct());
        final SpectrumChooser spectrumChooser = new SpectrumChooser(getPaneWindow(), allSpectra);
        if (spectrumChooser.show() == ModalDialog.ID_OK) {
            final DisplayableSpectrum[] spectra = spectrumChooser.getSpectra();
            productToAllSpectraMap.put(currentProduct, spectra);
        }
    }

    boolean isShowingCursorSpectrum() {
        return showSpectrumForCursorButton.isSelected();
    }

    private boolean isShowingPinSpectra() {
        return isShowingSpectraForSelectedPins() || isShowingSpectraForAllPins();
    }

    private boolean isShowingSpectraForAllPins() {
        return showSpectraForAllPinsButton.isSelected();
    }

    private void recreateChart() {
        chartHandler.updateData();
        chartHandler.updateChart();
        chartPanel.repaint();
        updateUIState();
    }

    Placemark[] getDisplayedPins() {
        if (isShowingSpectraForSelectedPins() && getCurrentView() != null) {
            return getCurrentView().getSelectedPins();
        } else if (isShowingSpectraForAllPins() && getCurrentProduct() != null) {
            ProductNodeGroup<Placemark> pinGroup = getCurrentProduct().getPinGroup();
            return pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        } else {
            return new Placemark[0];
        }
    }

    private void setUpSpectra() {
        DisplayableSpectrum[] spectra;
        if (!areSpectralBandsAvailable()) {
            spectra = new DisplayableSpectrum[]{};
        } else {
            final Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
            if (autoGrouping != null) {
                final int selectedSpectrumIndex = autoGrouping.indexOf(getCurrentView().getRaster().getName());
                spectra = new DisplayableSpectrum[autoGrouping.size() + 1];
                final Iterator<String[]> iterator = autoGrouping.iterator();
                int i = 0;
                while (iterator.hasNext()) {
                    final String spectrumName = iterator.next()[0];
                    int symbolIndex = SpectrumShapeProvider.getValidIndex(i, false);
                    DisplayableSpectrum spectrum = new DisplayableSpectrum(spectrumName, symbolIndex);
                    spectrum.setSelected(i == selectedSpectrumIndex);
                    spectrum.setLineStyle(SpectrumStrokeProvider.getStroke(i));
                    spectra[i++] = spectrum;
                }
                int symbolIndex = SpectrumShapeProvider.getValidIndex(i, false);
                DisplayableSpectrum defaultSpectrum =
                        new DisplayableSpectrum(DisplayableSpectrum.REMAINING_BANDS_NAME, symbolIndex);
                defaultSpectrum.setSelected(selectedSpectrumIndex == -1);
                spectra[spectra.length - 1] = defaultSpectrum;
                final SpectrumBand[] availableSpectralBands = getAvailableSpectralBands();
                for (SpectrumBand availableSpectralBand : availableSpectralBands) {
                    final String bandName = availableSpectralBand.getName();
                    final int spectrumIndex = autoGrouping.indexOf(bandName);
                    if (spectrumIndex != -1) {
                        spectra[spectrumIndex].addBand(availableSpectralBand);
                    } else {
                        spectra[spectra.length - 1].addBand(availableSpectralBand);
                    }
                }
            } else {
                spectra = new DisplayableSpectrum[1];
                spectra[0] = new DisplayableSpectrum(
                        DisplayableSpectrum.DEFAULT_SPECTRUM_NAME, getAvailableSpectralBands(), 1);
                spectra[0].setLineStyle(SpectrumStrokeProvider.getStroke(0));
            }
        }
        productToAllSpectraMap.put(currentProduct, spectra);
    }

    private DisplayableSpectrum[] getAllSpectra() {
        return productToAllSpectraMap.get(currentProduct);
    }

    private boolean areSpectralBandsAvailable() {
        return getAvailableSpectralBands().length > 0;
    }

    private boolean isShowingSpectraForSelectedPins() {
        return showSpectraForSelectedPinsButton.isSelected();
    }

    // todo - not yet implemented for 4.1 but planned for 5.0 (tf - 5.3.2014)
//    private boolean isShowingAveragePinSpectrum() {
//        return showAveragePinSpectrumButton.isSelected();
//    }

    List<DisplayableSpectrum> getSelectedSpectra() {
        List<DisplayableSpectrum> selectedSpectra = new ArrayList<DisplayableSpectrum>();
        if (currentProduct != null && productToAllSpectraMap.containsKey(currentProduct)) {
            DisplayableSpectrum[] allSpectra = productToAllSpectraMap.get(currentProduct);
            for (DisplayableSpectrum displayableSpectrum : allSpectra) {
                if (displayableSpectrum.isSelected()) {
                    selectedSpectra.add(displayableSpectrum);
                }
            }
        }
        return selectedSpectra;
    }

    private void addBandToSpectra(Band band) {
        DisplayableSpectrum[] allSpectra = productToAllSpectraMap.get(getCurrentProduct());
        Product.AutoGrouping autoGrouping = currentProduct.getAutoGrouping();
        if (autoGrouping != null) {
            final int bandIndex = autoGrouping.indexOf(band.getName());
            final DisplayableSpectrum spectrum;
            if (bandIndex != -1) {
                spectrum = allSpectra[bandIndex];
            } else {
                spectrum = allSpectra[allSpectra.length - 1];
            }
            spectrum.addBand(new SpectrumBand(band, spectrum.isSelected()));
        } else {
            allSpectra[0].addBand(new SpectrumBand(band, true));
        }
    }

    private void removeBandFromSpectra(Band band) {
        DisplayableSpectrum[] allSpectra = productToAllSpectraMap.get(currentProduct);
        for (DisplayableSpectrum displayableSpectrum : allSpectra) {
            Band[] spectralBands = displayableSpectrum.getSpectralBands();
            for (int j = 0; j < spectralBands.length; j++) {
                Band spectralBand = spectralBands[j];
                if (spectralBand == band) {
                    displayableSpectrum.remove(j);
                    if (displayableSpectrum.getSelectedBands().length == 0) {
                        displayableSpectrum.setSelected(false);
                    }
                    return;
                }
            }
        }
    }

    private void updateSpectraUnits() {
        for (DisplayableSpectrum spectrum : getAllSpectra()) {
            spectrum.updateUnit();
        }
    }

    void removeCursorSpectraFromDataset() {
        chartHandler.removeCursorSpectraFromDataset();
    }

    boolean hasDiagram() {
        return chartHandler.hasDiagram();
    }

    private void handleViewActivated(final ProductSceneView view) {
        view.addPixelPositionListener(pixelPositionListener);
        setCurrentView(view);
    }

    private void handleViewDeactivated(final ProductSceneView view) {
        view.removePixelPositionListener(pixelPositionListener);
        setCurrentView(null);
    }

    public boolean hasValidCursorPosition() {
        return chartHandler.hasValidCursorPosition();
    }

    private class ChartHandler {

        private static final String MESSAGE_NO_SPECTRAL_BANDS = "No spectral bands available";   /*I18N*/
        private static final String MESSAGE_NO_PRODUCT_SELECTED = "No product selected";
        private static final String MESSAGE_NO_SPECTRA_SELECTED = "No spectra selected";
        private static final String MESSAGE_COLLECTING_SPECTRAL_INFORMATION = "Collecting spectral information...";

        private final JFreeChart chart;
        private final ChartUpdater chartUpdater;

        private ChartHandler(JFreeChart chart) {
            chartUpdater = new ChartUpdater();
            this.chart = chart;
            setLegend(chart);
            setAutomaticRangeAdjustments(false);
            final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(false);
            setPlotMessage(MESSAGE_NO_PRODUCT_SELECTED);
        }

        private void setAutomaticRangeAdjustments(boolean userInducesAutomaticAdjustment) {
            final XYPlot plot = chart.getXYPlot();
            boolean adjustmentHasChanged = false;
            if (userInducesAutomaticAdjustment) {
                if (!isUserInducedAutomaticAdjustmentChosen) {
                    isUserInducedAutomaticAdjustmentChosen = true;
                    if (!isAutomaticDomainAdjustmentSet()) {
                        plot.getDomainAxis().setAutoRange(true);
                        domainAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                    if (!isAutomaticRangeAdjustmentSet()) {
                        plot.getRangeAxis().setAutoRange(true);
                        rangeAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                }
            } else {
                if (isUserInducedAutomaticAdjustmentChosen) {
                    isUserInducedAutomaticAdjustmentChosen = false;
                    if (isAutomaticDomainAdjustmentSet()) {
                        plot.getDomainAxis().setAutoRange(false);
                        domainAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                    if (isAutomaticRangeAdjustmentSet()) {
                        plot.getRangeAxis().setAutoRange(false);
                        rangeAxisAdjustmentIsFrozen = false;
                        adjustmentHasChanged = true;
                    }
                }
            }
            if (adjustmentHasChanged) {
                chartUpdater.invalidatePlotBounds();
            }
        }

        private boolean isAutomaticDomainAdjustmentSet() {
            return chart.getXYPlot().getDomainAxis().isAutoRange();
        }

        private boolean isAutomaticRangeAdjustmentSet() {
            return chart.getXYPlot().getRangeAxis().isAutoRange();
        }

        private void setLegend(JFreeChart chart) {
            chart.removeLegend();
            final LegendTitle legend = new LegendTitle(new SpectrumLegendItemSource());
            legend.setPosition(RectangleEdge.BOTTOM);
            LineBorder border = new LineBorder(Color.BLACK, new BasicStroke(), new RectangleInsets(2, 2, 2, 2));
            legend.setFrame(border);
            chart.addLegend(legend);
        }

        private void setPosition(int pixelX, int pixelY) {
            chartUpdater.setPosition(pixelX, pixelY);
        }

        private void setLevel(int level) {
            chartUpdater.setLevel(level);
        }

        private void updateChart() {
            if (chartUpdater.isDatasetEmpty()) {
                setEmptyPlot();
                return;
            }
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
            chartUpdater.updateChart(chart, spectra);
            chart.getXYPlot().clearAnnotations();
        }

        private void updateData() {
            List<DisplayableSpectrum> spectra = getSelectedSpectra();
            chartUpdater.updateData(chart, spectra);
        }

        private void setEmptyPlot() {
            chart.getXYPlot().setDataset(null);
            if (getCurrentProduct() == null) {
                setPlotMessage(MESSAGE_NO_PRODUCT_SELECTED);
            } else if (!chartUpdater.hasValidCursorPosition()) {
                return;
            } else if (getAllSpectra().length == 0) {
                setPlotMessage(MESSAGE_NO_SPECTRA_SELECTED);
            } else {
                setPlotMessage(MESSAGE_NO_SPECTRAL_BANDS);
            }
        }

        private void setGridVisible(boolean visible) {
            chart.getXYPlot().setDomainGridlinesVisible(visible);
            chart.getXYPlot().setRangeGridlinesVisible(visible);
        }

        private boolean hasDiagram() {
            return chart.getXYPlot().getDataset() != null;
        }

        private void removePinInformation(Placemark pin) {
            chartUpdater.removePinInformation(pin);
        }

        private void removeBandInformation(Band band) {
            chartUpdater.removeBandinformation(band);
        }

        private void setPlotMessage(String messageText) {
            chart.getXYPlot().clearAnnotations();
            TextTitle tt = new TextTitle(messageText);
            tt.setTextAlignment(HorizontalAlignment.RIGHT);
            tt.setFont(chart.getLegend().getItemFont());
            tt.setBackgroundPaint(new Color(200, 200, 255, 50));
            tt.setFrame(new BlockBorder(Color.white));
            tt.setPosition(RectangleEdge.BOTTOM);
            XYTitleAnnotation message = new XYTitleAnnotation(0.5, 0.5, tt, RectangleAnchor.CENTER);
            chart.getXYPlot().addAnnotation(message);
        }

        public boolean hasValidCursorPosition() {
            return chartUpdater.hasValidCursorPosition();
        }

        public void removeCursorSpectraFromDataset() {
            chartUpdater.removeCursorSpectraFromDataset();
        }

        public void setCollectingSpectralInformationMessage() {
            setPlotMessage(MESSAGE_COLLECTING_SPECTRAL_INFORMATION);
        }
    }

    private class ChartUpdater {

        private final static int domain_axis_index = 0;
        private final static int range_axis_index = 1;
        private final static double relativePlotInset = 0.05;

        private final Map<Placemark, Map<Band, Double>> pinToEnergies;
        private int pixelX;
        private int pixelY;
        private int level;
        private Range[] plotBounds;
        private XYSeriesCollection dataset;

        private ChartUpdater() {
            pinToEnergies = new HashMap<Placemark, Map<Band, Double>>();
            plotBounds = new Range[2];
            invalidatePlotBounds();
        }

        void invalidatePlotBounds() {
            plotBounds[domain_axis_index] = null;
            plotBounds[range_axis_index] = null;
        }

        private void setLevel(int level) {
            this.level = level;
        }

        private void setPosition(int pixelX, int pixelY) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
        }

        private void updateData(JFreeChart chart, List<DisplayableSpectrum> spectra) {
            dataset = new XYSeriesCollection();
            if (level >= 0) {
                fillDatasetWithPinSeries(spectra, dataset, chart);
                // todo - not yet implemented for 4.1 but planned for 5.0 (tf - 5.3.2014)
//                fillDatasetWithAveragePinSpectrum(spectra, dataset, chart);
                if (hasValidCursorPosition()) {
                    fillDatasetWithCursorSeries(spectra, dataset, chart);
                }
            }
        }

        private void updateChart(JFreeChart chart, List<DisplayableSpectrum> spectra) {
            final XYPlot plot = chart.getXYPlot();
            if (!chartHandler.isAutomaticDomainAdjustmentSet() && !domainAxisAdjustmentIsFrozen) {
                isCodeInducedAxisChange = true;
                updatePlotBounds(dataset.getDomainBounds(true), plot.getDomainAxis(), domain_axis_index);
                isCodeInducedAxisChange = false;
            }
            if (!chartHandler.isAutomaticRangeAdjustmentSet() && !rangeAxisAdjustmentIsFrozen) {
                isCodeInducedAxisChange = true;
                updatePlotBounds(dataset.getRangeBounds(true), plot.getRangeAxis(), range_axis_index);
                isCodeInducedAxisChange = false;
            }
            plot.setDataset(dataset);
            setPlotUnit(spectra, plot);
        }

        private void setPlotUnit(List<DisplayableSpectrum> spectra, XYPlot plot) {
            String unitToBeDisplayed = spectra.get(0).getUnit();
            int i = 1;
            while (i < spectra.size() && !unitToBeDisplayed.equals(DisplayableSpectrum.MIXED_UNITS)) {
                DisplayableSpectrum displayableSpectrum = spectra.get(i++);
                if (displayableSpectrum.hasSelectedBands() && !unitToBeDisplayed.equals(displayableSpectrum.getUnit())) {
                    unitToBeDisplayed = DisplayableSpectrum.MIXED_UNITS;
                }
            }
            isCodeInducedAxisChange = true;
            plot.getRangeAxis().setLabel(unitToBeDisplayed);
            isCodeInducedAxisChange = false;
        }

        private void updatePlotBounds(Range newBounds, ValueAxis axis, int index) {
            if (newBounds != null) {
                final Range axisBounds = axis.getRange();
                final Range oldBounds = this.plotBounds[index];
                this.plotBounds[index] = getNewRange(newBounds, this.plotBounds[index], axisBounds);
                if (oldBounds != this.plotBounds[index]) {
                    axis.setRange(getNewPlotBounds(this.plotBounds[index]));
                }
            }
        }

        private Range getNewRange(Range newBounds, Range currentBounds, Range plotBounds) {
            if (currentBounds == null) {
                currentBounds = newBounds;
            } else {
                if (plotBounds.getLowerBound() > 0 && newBounds.getLowerBound() < currentBounds.getLowerBound() ||
                        newBounds.getUpperBound() > currentBounds.getUpperBound()) {
                    currentBounds = new Range(Math.min(currentBounds.getLowerBound(), newBounds.getLowerBound()),
                                              Math.max(currentBounds.getUpperBound(), newBounds.getUpperBound()));
                }
            }
            return currentBounds;
        }

        private Range getNewPlotBounds(Range bounds) {
            double range = bounds.getLength();
            double delta = range * relativePlotInset;
            return new Range(Math.max(0, bounds.getLowerBound() - delta),
                             bounds.getUpperBound() + delta);
        }

        private void fillDatasetWithCursorSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
            if (isShowingCursorSpectrum() && getCurrentView().isCurrentPixelPosValid()) {
                for (DisplayableSpectrum spectrum : spectra) {
                    XYSeries series = new XYSeries(spectrum.getName());
                    final Band[] spectralBands = spectrum.getSelectedBands();
                    for (Band spectralBand : spectralBands) {
                        final float wavelength = spectralBand.getSpectralWavelength();
                        final double energy = ProductUtils.getGeophysicalSampleDouble(spectralBand, pixelX, pixelY, level);
                        if (energy != spectralBand.getGeophysicalNoDataValue()) {
                            series.add(wavelength, energy);
                        }
                    }
                    updateRenderer(dataset.getSeriesCount(), Color.BLACK, spectrum, chart);
                    dataset.addSeries(series);
                }
            }
        }

        private void fillDatasetWithPinSeries(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
            Placemark[] pins = getDisplayedPins();
            for (Placemark pin : pins) {
                List<XYSeries> pinSeries = createXYSeriesFromPin(pin, dataset.getSeriesCount(), spectra, chart);
                for (XYSeries series : pinSeries) {
                    dataset.addSeries(series);
                }
            }
        }

        private List<XYSeries> createXYSeriesFromPin(Placemark pin, int seriesIndex, List<DisplayableSpectrum> spectra, JFreeChart chart) {
            List<XYSeries> pinSeries = new ArrayList<XYSeries>();
            Color pinColor = PlacemarkUtils.getPlacemarkColor(pin, getCurrentView());
            for (DisplayableSpectrum spectrum : spectra) {
                XYSeries series = new XYSeries(spectrum.getName() + "_" + pin.getLabel());
                final Band[] spectralBands = spectrum.getSelectedBands();
                Map<Band, Double> bandToEnergy;
                if (pinToEnergies.containsKey(pin)) {
                    bandToEnergy = pinToEnergies.get(pin);
                } else {
                    bandToEnergy = new HashMap<Band, Double>();
                    pinToEnergies.put(pin, bandToEnergy);
                }
                for (Band spectralBand : spectralBands) {
                    double energy;
                    if (bandToEnergy.containsKey(spectralBand)) {
                        energy = bandToEnergy.get(spectralBand);
                    } else {
                        energy = readEnergy(pin, spectralBand);
                        bandToEnergy.put(spectralBand, energy);
                    }
                    final float wavelength = spectralBand.getSpectralWavelength();
                    if (energy != spectralBand.getGeophysicalNoDataValue()) {
                        series.add(wavelength, energy);
                    }
                }
                updateRenderer(seriesIndex++, pinColor, spectrum, chart);
                pinSeries.add(series);
            }
            return pinSeries;
        }

        // todo - not yet implemented for 4.1 but planned for 5.0 (tf - 5.3.2014)
//        private void fillDatasetWithAveragePinSpectrum(List<DisplayableSpectrum> spectra, XYSeriesCollection dataset, JFreeChart chart) {
//            if(!isShowingAveragePinSpectrum()) {
//                return;
//            }
//            ProductNodeGroup<Placemark> pinGroup = getCurrentProduct().getPinGroup();
//            for(int i = 0; i < pinGroup.getNodeCount(); i++) {
//                Placemark pin = pinGroup.get(i);
//                for (DisplayableSpectrum spectrum : spectra) {
////                    XYSeries series = new XYSeries(spectrum.getName() + "_" + pin.getLabel());
//                    final Band[] spectralBands = spectrum.getSelectedBands();
//                    Map<Band, Double> bandToEnergy;
//                    if (pinToEnergies.containsKey(pin)) {
//                        bandToEnergy = pinToEnergies.get(pin);
//                    } else {
//                        bandToEnergy = new HashMap<Band, Double>();
//                        pinToEnergies.put(pin, bandToEnergy);
//                    }
//                    for (Band spectralBand : spectralBands) {
//                        double energy;
//                        if (bandToEnergy.containsKey(spectralBand)) {
//                            energy = bandToEnergy.get(spectralBand);
//                        } else {
//                            energy = readEnergy(pin, spectralBand);
//                            bandToEnergy.put(spectralBand, energy);
//                        }
//                        final float wavelength = spectralBand.getSpectralWavelength();
//                        if (energy != spectralBand.getGeophysicalNoDataValue()) {
//                            series.add(wavelength, energy);
//                        }
//                    }
////                    updateRenderer(seriesIndex++, pinColor, spectrum, chart);
////                    pinSeries.add(series);
//                }
//            }
//
//        }

        private void updateRenderer(int seriesIndex, Color seriesColor, DisplayableSpectrum spectrum, JFreeChart chart) {
            final XYItemRenderer renderer = chart.getXYPlot().getRenderer();
            renderer.setSeriesPaint(seriesIndex, seriesColor);
            final Stroke lineStyle = spectrum.getLineStyle();
            renderer.setSeriesStroke(seriesIndex, lineStyle);
            Shape symbol = spectrum.getScaledShape();
            renderer.setSeriesShape(seriesIndex, symbol);
        }

        private double readEnergy(Placemark pin, Band spectralBand) {
            final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(spectralBand);
            final AffineTransform i2mTransform = multiLevelModel.getImageToModelTransform(0);
            final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
            final Point2D modelPixel = i2mTransform.transform(pin.getPixelPos(), null);
            final Point2D imagePixel = m2iTransform.transform(modelPixel, null);
            int pinPixelX = (int) Math.floor(imagePixel.getX());
            int pinPixelY = (int) Math.floor(imagePixel.getY());
            return ProductUtils.getGeophysicalSampleDouble(spectralBand, pinPixelX, pinPixelY, level);
        }

        private void removePinInformation(Placemark pin) {
            pinToEnergies.remove(pin);
        }

        private void removeBandinformation(Band band) {
            for (Placemark pin : pinToEnergies.keySet()) {
                Map<Band, Double> bandToEnergiesMap = pinToEnergies.get(pin);
                if (bandToEnergiesMap.containsKey(band)) {
                    bandToEnergiesMap.remove(band);
                }
            }
        }

        public boolean hasValidCursorPosition() {
            return pixelX > Integer.MIN_VALUE && pixelY > Integer.MIN_VALUE;
        }

        void removeCursorSpectraFromDataset() {
            if (hasValidCursorPosition()) {
                pixelX = Integer.MIN_VALUE;
                pixelY = Integer.MIN_VALUE;
                int numberOfSelectedSpectra = getSelectedSpectra().size();
                int numberOfPins = getDisplayedPins().length;
                int numberOfDisplayedGraphs = numberOfPins * numberOfSelectedSpectra;
                while (dataset.getSeriesCount() > numberOfDisplayedGraphs) {
                    dataset.removeSeries(dataset.getSeriesCount() - 1);
                }
            }
        }

        public boolean isDatasetEmpty() {
            return dataset == null || dataset.getSeriesCount() == 0;
        }

    }

    private class SpectrumLegendItemSource implements LegendItemSource {

        @Override
        public LegendItemCollection getLegendItems() {
            LegendItemCollection itemCollection = new LegendItemCollection();
            final Placemark[] displayedPins = getDisplayedPins();
            final List<DisplayableSpectrum> spectra = getSelectedSpectra();
            for (Placemark pin : displayedPins) {
                Paint pinPaint = PlacemarkUtils.getPlacemarkColor(pin, getCurrentView());
                for (DisplayableSpectrum spectrum : spectra) {
                    if (spectrum.hasSelectedBands()) {
                        String legendLabel = pin.getLabel() + "_" + spectrum.getName();
                        LegendItem item = createLegendItem(spectrum, pinPaint, legendLabel);
                        itemCollection.add(item);
                    }
                }
            }
            if (isShowingCursorSpectrum() && hasValidCursorPosition()) {
                for (DisplayableSpectrum spectrum : spectra) {
                    if (spectrum.hasSelectedBands()) {
                        Paint defaultPaint = Color.BLACK;
                        LegendItem item = createLegendItem(spectrum, defaultPaint, spectrum.getName());
                        itemCollection.add(item);
                    }
                }
            }
            return itemCollection;
        }

        private LegendItem createLegendItem(DisplayableSpectrum spectrum, Paint paint, String legendLabel) {
            Stroke outlineStroke = new BasicStroke();
            Line2D lineShape = new Line2D.Double(0, 5, 40, 5);
            Stroke lineStyle = spectrum.getLineStyle();
            Shape symbol = spectrum.getScaledShape();
            return new LegendItem(legendLabel, legendLabel, legendLabel, legendLabel,
                                  true, symbol, false,
                                  paint, true, paint, outlineStroke,
                                  true, lineShape, lineStyle, paint);
        }

    }

/////////////////////////////////////////////////////////////////////////
// View change handling

    private class SpectrumIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                handleViewActivated((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                handleViewDeactivated((ProductSceneView) contentPane);
            }
        }
    }

/////////////////////////////////////////////////////////////////////////
// Product change handling

    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            boolean chartHasChanged = false;
            if (event.getSourceNode() instanceof Band) {
                final String propertyName = event.getPropertyName();
                if (propertyName.equals(DataNode.PROPERTY_NAME_UNIT)) {
                    updateSpectraUnits();
                    chartHasChanged = true;
                } else if (propertyName.equals(Band.PROPERTY_NAME_SPECTRAL_WAVELENGTH)) {
                    setUpSpectra();
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (event.getPropertyName().equals("geoPos") || event.getPropertyName().equals("pixelPos")) {
                    chartHandler.removePinInformation((Placemark) event.getSourceNode());
                }
                if (isShowingPinSpectra()) {
                    chartHasChanged = true;
                }
            } else if (event.getSourceNode() instanceof Product) {
                if (event.getPropertyName().equals("autoGrouping")) {
                    setUpSpectra();
                    chartHasChanged = true;
                }
            }
            if (isActive() && chartHasChanged) {
                recreateChart();
            }
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                Band newBand = (Band) event.getSourceNode();
                if (isSpectralBand(newBand)) {
                    addBandToSpectra((Band) event.getSourceNode());
                    recreateChart();
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateChart();
                } else {
                    updateUIState();
                }
            }
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            if (!isActive()) {
                return;
            }
            if (event.getSourceNode() instanceof Band) {
                Band band = (Band) event.getSourceNode();
                removeBandFromSpectra(band);
                chartHandler.removeBandInformation(band);
                recreateChart();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateChart();
                }
            }
        }

        private boolean isActive() {
            return isVisible() && getCurrentProduct() != null;
        }
    }

    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            recreateChart();
        }

    }

}
