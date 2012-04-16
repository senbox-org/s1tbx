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
package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.diagram.DiagramCanvas;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A window which displays product spectra.
 */
public class SpectrumToolView extends AbstractToolView {

    public static final String ID = SpectrumToolView.class.getName();
    
    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";
    private static final String MSG_NO_SPECTRAL_BANDS = "No spectral bands.";   /*I18N*/

    private final Map<Product, SpectraDiagram> productToDiagramMap;
    private final ProductNodeListenerAdapter productNodeHandler;
    private final PinSelectionChangeListener pinSelectionChangeListener;
    private final CursorSpectrumPPL ppl;
    
    private DiagramCanvas diagramCanvas;
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
    private int pixelX;
    private int pixelY;
    private int level;

    public SpectrumToolView() {
        productNodeHandler = new ProductNodeHandler();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        productToDiagramMap = new HashMap<Product, SpectraDiagram>(4);
        ppl = new CursorSpectrumPPL();
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
                // reset stored pixel location, may be invalid for new product
                pixelX = 0;
                pixelY = 0;
                SpectraDiagram spectraDiagram = getSpectraDiagram();
                if (spectraDiagram != null) {
                    diagramCanvas.setDiagram(spectraDiagram);
                } else {
                    recreateSpectraDiagram();
                }
            }
            if (currentProduct == null) {
                diagramCanvas.setDiagram(null);
                diagramCanvas.setMessageText("No product selected."); /*I18N*/
            } else {
                diagramCanvas.setMessageText(null);
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
        boolean hasDiagram = diagramCanvas.getDiagram() != null;
        filterButton.setEnabled(hasProduct);
        showSpectrumForCursorButton.setEnabled(hasView);
        showSpectraForSelectedPinsButton.setEnabled(hasSelectedPins);
        showSpectraForAllPinsButton.setEnabled(hasPins);
        showGridButton.setEnabled(hasDiagram);
// todo - not yet implemented for 4.1 but planned for 4.2 (mp - 31.10.2007)
//        showAveragePinSpectrumButton.setEnabled(hasPins); // todo - hasSpectraGraphs
//        showGraphPointsButton.setEnabled(hasDiagram);
        diagramCanvas.setEnabled(hasProduct);    // todo - hasSpectraGraphs

        if (hasDiagram) {
            showGridButton.setSelected(diagramCanvas.getDiagram().getDrawGrid());
        }
    }

    private void updateSpectra(int pixelX, int pixelY, int level) {
        maybeShowTip();
        diagramCanvas.setMessageText(null);
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.level = level;
        SpectraDiagram spectraDiagram = getSpectraDiagram();
        if (spectraDiagram.getBands().length > 0) {
            spectraDiagram.updateSpectra(pixelX, pixelY, level);
        } else {
            diagramCanvas.setMessageText(MSG_NO_SPECTRAL_BANDS);
        }
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

    private Band[] getSelectedSpectralBands() {
        return getSpectraDiagram().getBands();
    }

    private Band[] getAvailableSpectralBands() {
        Debug.assertNotNull(getCurrentProduct());
        Band[] bands = getCurrentProduct().getBands();
        ArrayList<Band> spectralBands = new ArrayList<Band>(15);
        for (Band band : bands) {
            if (band.getSpectralWavelength() > 0.0) {
                if (!band.isFlagBand()) {
                    spectralBands.add(band);
                }
            }
        }
        return spectralBands.toArray(new Band[spectralBands.size()]);
    }

    @Override
    public JComponent createControl() {
        titleBase = getDescriptor().getTitle();
        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"), false);
        filterButton.setName("filterButton");
        filterButton.setEnabled(false);
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectSpectralBands();
            }
        });

        showSpectrumForCursorButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/CursorSpectrum24.gif"), true);
        showSpectrumForCursorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recreateSpectraDiagram();
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
                }
                recreateSpectraDiagram();
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
                }
                recreateSpectraDiagram();
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
//                JOptionPane.showMessageDialog(null, "Not implemented");
//            }
//        });
//        showAveragePinSpectrumButton.setName("showAveragePinSpectrumButton");
//        showAveragePinSpectrumButton.setToolTipText("Show average spectrum of all pin spectra.");

        showGridButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SpectrumGrid24.gif"), true);
        showGridButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (diagramCanvas.getDiagram() != null) {
                    diagramCanvas.getDiagram().setDrawGrid(showGridButton.isSelected());
                }
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

        diagramCanvas = new DiagramCanvas();
        diagramCanvas.setPreferredSize(new Dimension(300, 200));
        diagramCanvas.setMessageText("No product selected."); /*I18N*/
        diagramCanvas.setBackground(Color.white);
        diagramCanvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, diagramCanvas);
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
                    setSpectraDiagram(null);
                    setCurrentView(null);
                    setCurrentProduct(null);
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
        Band[] allBandNames = getAvailableSpectralBands();
        Band[] selectedBands = getSelectedSpectralBands();
        if (selectedBands == null) {
            selectedBands = allBandNames;
        }
        BandChooser bandChooser = new BandChooser(getPaneWindow(), "Available Spectral Bands",
                                                  getDescriptor().getHelpId(),
                                                  allBandNames, selectedBands);
        if (bandChooser.show() == ModalDialog.ID_OK) {
            Band[] userSelectedBands = bandChooser.getSelectedBands();
            boolean userSelection = (userSelectedBands.length != allBandNames.length);
            getSpectraDiagram().setBands(userSelectedBands, userSelection);
        }
        updateUIState();
    }

    SpectraDiagram getSpectraDiagram() {
        Debug.assertNotNull(currentProduct);
        return productToDiagramMap.get(currentProduct);
    }

    private void setSpectraDiagram(final SpectraDiagram newDiagram) {
        Debug.assertNotNull(currentProduct);
        SpectraDiagram oldDiagram;
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

    private boolean isShowingCursorSpectrum() {
        return showSpectrumForCursorButton.isSelected();
    }

    private boolean isShowingPinSpectra() {
        return isShowingSpectraForSelectedPins() || isShowingSpectraForAllPins();
    }

    private boolean isShowingSpectraForAllPins() {
        return showSpectraForAllPinsButton.isSelected();
    }

    private void recreateSpectraDiagram() {
        SpectraDiagram spectraDiagram = new SpectraDiagram(getCurrentProduct());

        if (isShowingSpectraForSelectedPins()) {
            Placemark[] pins = getCurrentView().getSelectedPins();
            for (Placemark pin : pins) {
                spectraDiagram.addSpectrumGraph(pin);
            }
        } else if (isShowingSpectraForAllPins()) {
            ProductNodeGroup<Placemark> pinGroup = getCurrentProduct().getPinGroup();
            Placemark[] pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
            for (Placemark pin : pins) {
                spectraDiagram.addSpectrumGraph(pin);
            }
        }

        if (isShowingCursorSpectrum()) {
            spectraDiagram.addCursorSpectrumGraph();
        }

        if (getSpectraDiagram() != null && getSelectedSpectralBands() != null && getSpectraDiagram().isUserSelection()) {
            spectraDiagram.setBands(getSelectedSpectralBands(), true);
        } else {
            spectraDiagram.setBands(getAvailableSpectralBands(), false);
        }
        spectraDiagram.updateSpectra(pixelX, pixelY, level);
        setSpectraDiagram(spectraDiagram);
        updateUIState();
    }

    private boolean isShowingSpectraForSelectedPins() {
        return showSpectraForSelectedPinsButton.isSelected();
    }

    private void handleViewActivated(final ProductSceneView view) {
        view.addPixelPositionListener(ppl);
        setCurrentView(view);
    }

    private void handleViewDeactivated(final ProductSceneView view) {
        view.removePixelPositionListener(ppl);
        setCurrentView(null);
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
    // Pixel position change handling

    private class CursorSpectrumPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    int pixelX,
                                    int pixelY,
                                    int currentLevel,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            diagramCanvas.setMessageText(null);
            if (pixelPosValid && isActive()) {
                getSpectraDiagram().addCursorSpectrumGraph();
                updateSpectra(pixelX, pixelY, currentLevel);
            }
            if (e.isShiftDown()) {
                getSpectraDiagram().adjustAxes(true);
            }
        }

        @Override
        public void pixelPosNotAvailable() {

            if (isActive()) {
                getSpectraDiagram().removeCursorSpectrumGraph();
                diagramCanvas.repaint();
            }
        }

        private boolean isActive() {
            return isVisible() && isShowingCursorSpectrum() && getSpectraDiagram() != null;
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
                    recreateSpectraDiagram();
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateSpectraDiagram();
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
                recreateSpectraDiagram();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateSpectraDiagram();
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
                recreateSpectraDiagram();
            } else if (event.getSourceNode() instanceof Placemark) {
                if (isShowingPinSpectra()) {
                    recreateSpectraDiagram();
                }
            }
            updateUIState();
        }

        private boolean isActive() {
            return isVisible() && getCurrentProduct() != null;
        }
    }
    
    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            recreateSpectraDiagram();
        }
        
    }

}
