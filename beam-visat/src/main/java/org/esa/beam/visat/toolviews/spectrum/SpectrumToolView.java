/*
 * $Id: SpectrumToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramCanvas;
import org.esa.beam.framework.ui.diagram.DiagramGraph;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

// @todo 1 nf/nf - add Popupmenu for data to clipboard export


/**
 * A window which displays product spectra.
 */
public class SpectrumToolView extends AbstractToolView {

    public static final String ID = SpectrumToolView.class.getName();
    private static final String SUPPRESS_MESSAGE_KEY = "plugin.spectrum.tip";

    private static final String MSG_NO_SPECTRAL_BANDS = "No spectral bands.";   /*I18N*/

    private Map<Product, PixelPositionListener> productToPixelPosListenerMap;
    private PinSelectionListener pinSelectionListener;
    private Product currentProduct;
    private final HashMap<Product, Diagram> productToDiagramMap;
    private final ProductNodeListenerAdapter propertyChangedHandler;
    private JCheckBox pinCheckbox;
    private DiagramCanvas diagramCanvas;
    private double xMinAuto;
    private double xMaxAuto;
    private double yMinAuto;
    private double yMaxAuto;
    private AbstractButton filterButton;
    private int currentX;
    private int currentY;
    private boolean tipShown;
    private String originalDescriptorTitel;


    public SpectrumToolView() {
        pinSelectionListener = new PinSelectionListener();
        productToDiagramMap = new HashMap<Product, Diagram>();
        propertyChangedHandler = createPropertyChangedHandler();

    }

    public void setDiagram(Diagram diagram) {
        diagramCanvas.setDiagram(diagram);
    }

    public void setInvalidDiagramMessage(String messageText) {
        diagramCanvas.setMessageText(messageText);
    }

    public void setDiagram(Diagram diagram, String messageText) {
        diagramCanvas.setDiagram(diagram);
    }

    public Product getCurrentProduct() {
        return currentProduct;
    }

    public void setCurrentProduct(Product currentProduct) {
        if (originalDescriptorTitel != null) {
            originalDescriptorTitel = getDescriptor().getTitle();
        }
        Product oldValue = this.currentProduct;
        if (oldValue != currentProduct) {
            if (oldValue != null) {
                oldValue.removeProductNodeListener(propertyChangedHandler);
            }
            this.currentProduct = currentProduct;
            if (this.currentProduct != null) {
                this.currentProduct.addProductNodeListener(propertyChangedHandler);
                Diagram diagram = getCurrentProductDiagram();
                if (diagram == null) {
                    diagram = new Diagram();
                    diagram.setXAxis(new DiagramAxis("Wavelength", "nm"));
                    diagram.setYAxis(new DiagramAxis("", "1"));
                    setCurrentProductDiagram(diagram);
                }
                if (getSpectrumGraph() == null) {
                    setSelectedBands(getSpectralBands());
                } else {
                    setAxesMinMaxAccumulatorsToAxesMinMax();
                }
                diagramCanvas.setDiagram(diagram);
                setTitle(getDescriptor().getTitle() + " - " + this.currentProduct.getProductRefString());
            } else {
                setTitle(getDescriptor().getTitle());
            }
            filterButton.setEnabled(this.currentProduct != null);
        }
    }

    public void updateSpectrum(final int pixelX, final int pixelY) {
        currentX = pixelX;
        currentY = pixelY;
        final SpectrumGraph spectrumGraph = getSpectrumGraph();
        if (spectrumGraph != null) {
            try {
                spectrumGraph.readValues(pixelX, pixelY);

                final DiagramAxis xAxis = getCurrentProductDiagram().getXAxis();
                xMinAuto = Math.min(xMinAuto, spectrumGraph.getXMin());
                xMaxAuto = Math.max(xMaxAuto, spectrumGraph.getXMax());
                boolean xRangeValid = xMaxAuto > xMinAuto;
                if (xRangeValid) {
                    xAxis.setValueRange(xMinAuto, xMaxAuto);
                    xAxis.setOptimalSubDivision(4, 6, 5);
                } else {
                    setInvalidDiagramMessage("All X-values are zero."); /*I18N*/
                }

                final DiagramAxis yAxis = getCurrentProductDiagram().getYAxis();
                yMinAuto = Math.min(yMinAuto, spectrumGraph.getYMin());
                yMaxAuto = Math.max(yMaxAuto, spectrumGraph.getYMax());
                boolean yRangeValid = yMaxAuto > yMinAuto;
                if (yRangeValid) {
                    yAxis.setValueRange(yMinAuto, yMaxAuto);
                    yAxis.setOptimalSubDivision(3, 6, 5);
                } else {
                    setInvalidDiagramMessage("All Y-values are zero."); /*I18N*/
                }
                if (xRangeValid && yRangeValid) {
                    setInvalidDiagramMessage(null);
                }
            } catch (IOException e) {
                setInvalidDiagramMessage("I/O error: " + e.getMessage());
            }
            if (isSnapToPinSelected() && getCurrentProduct().getSelectedPin() == null) {
                setNoPinSelected();
            }
            diagramCanvas.repaint();
        } else {
            diagramCanvas.setMessageText(MSG_NO_SPECTRAL_BANDS);
        }
    }

    public void setSpectrumToSelectedPin() {
        final Product currentProduct = getCurrentProduct();
        if (currentProduct == null) {
            return;
        }
        final Pin selectedPin = currentProduct.getSelectedPin();
        if (selectedPin != null) {
            setSpectrumForPin(selectedPin);
        } else {
            setNoPinSelected();
        }
    }

    public void setSpectrumForPin(final Pin pin) {
        Guardian.assertNotNull("pin", pin);
        final PixelPos pos = pin.getPixelPos();
        final int x = MathUtils.floorInt(pos.x);
        final int y = MathUtils.floorInt(pos.y);
        updateSpectrum(x, y);
    }

    public void setAxesMinMaxAccumulatorsToAxesMinMax() {
        Diagram diagram = getCurrentProductDiagram();
        if (diagram != null) {
            Debug.assertNotNull(diagram.getXAxis());
            Debug.assertNotNull(diagram.getYAxis());
            xMinAuto = diagram.getXAxis().getMinValue();
            xMaxAuto = diagram.getXAxis().getMaxValue();
            yMinAuto = diagram.getYAxis().getMinValue();
            yMaxAuto = diagram.getYAxis().getMaxValue();
        }
    }

    public boolean isSnapToPinSelected() {
        return pinCheckbox.isSelected();
    }

    public void setNoPinSelected() {
        setInvalidDiagramMessage("No pin selected.");
    }

    private SpectrumGraph getSpectrumGraph() {
        final Diagram diagram = getCurrentProductDiagram();
        if (diagram != null) {
            return (SpectrumGraph) diagram.getGraph();
        }
        return null;
    }


    private Band[] getSelectedBands() {
        final SpectrumGraph spectrumValues = getSpectrumGraph();
        if (spectrumValues != null) {
            return spectrumValues.getBands();
        }
        return null;
    }

    private void setSelectedBands(Band[] selectedBands) {
        Diagram diagram = getCurrentProductDiagram();
        Debug.assertTrue(diagram != null);
        if (selectedBands == null || selectedBands.length == 0) {
            diagramCanvas.setMessageText("No spectral bands."); /*I18N*/
//            diagram.setValues(null);
        } else {
            SpectrumGraph spectrumValues = (SpectrumGraph) diagram.getGraph();
            if (spectrumValues != null) {
                spectrumValues.setBands(selectedBands);
            } else {
                spectrumValues = new SpectrumGraph(selectedBands);
                diagram.setGraph(spectrumValues);
            }
            updateYUnit(selectedBands);
            resetAxesMinMaxAccumulators();
        }
    }

    private void updateYUnit(final Band[] selectedBands) {
        final Diagram diagram = getCurrentProductDiagram();
        diagram.getYAxis().setUnit(getUnit(selectedBands));
        diagramCanvas.repaint();
    }

    private static String getUnit(final Band[] bands) {
        String unit = null;
        for (final Band band : bands) {
            if (unit == null) {
                unit = band.getUnit();
            } else if (!unit.equals(band.getUnit())) {
                unit = " mixed units "; /*I18N*/
                break;
            }
        }
        return unit != null ? unit : "?";
    }

    private Band[] getSpectralBands() {
        Debug.assertNotNull(currentProduct);
        Band[] bands = currentProduct.getBands();
        Vector<Band> spectralBands = new Vector<Band>();
        for (Band band : bands) {
            if (band.getSpectralWavelength() > 0.0) {
                spectralBands.add(band);
            }
        }
        return spectralBands.toArray(new Band[spectralBands.size()]);
    }

    @Override
    public void componentOpened() {
        if (!tipShown) {
            VisatApp.getApp().showInfoDialog("Spectrum Tip",
                                             "Tip: If you press the SHIFT key while moving the mouse cursor over \n" +
                                             "an image, VISAT adjusts the diagram axes to the local values at the\n" +
                                             "current pixel position, if you release the SHIFT key again, then the\n" +
                                             "min/max are accumulated again.", /*I18N*/
                                                                               SUPPRESS_MESSAGE_KEY);
            tipShown = true;
        }
    }

    @Override
    public JComponent createControl() {
        pinCheckbox = new JCheckBox("Snap to selected pin");
        pinCheckbox.setSelected(false);

        pinCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pinCheckbox.isSelected()) {
                    resetAxesMinMaxAccumulators();
                    setSpectrumToSelectedPin();
                }
            }
        });

        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"), false);
        filterButton.setEnabled(false);
        filterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentProduct != null) {
                    Band[] allBandNames = getSpectralBands();
                    Band[] selectedBands = getSelectedBands();
                    if (selectedBands == null) {
                        selectedBands = allBandNames;
                    }
                    BandChooser bandChooser = new BandChooser(getWindowAncestor(), "Available Spectral Bands",
                                                              getDescriptor().getHelpId(),
                                                              allBandNames, selectedBands);
                    if (bandChooser.show() == ModalDialog.ID_OK) {
                        setSelectedBands(bandChooser.getSelectedBands());
                        updateSpectrum(currentX, currentY);
                    }
                }
            }
        });

        AbstractButton _helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        _helpButton.setToolTipText("Help."); /*I18N*/

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.bottom = 3;
        buttonPane.add(filterButton, gbc);
        gbc.insets.bottom = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPane.add(_helpButton, gbc);

        diagramCanvas = new DiagramCanvas();
        diagramCanvas.setPreferredSize(new Dimension(300, 200));
        diagramCanvas.setMessageText("No product selected."); /*I18N*/
        diagramCanvas.setBackground(Color.white);
        diagramCanvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        JPanel diagramPanel = new JPanel(new BorderLayout());
        diagramPanel.add(BorderLayout.CENTER, diagramCanvas);
        diagramPanel.add(BorderLayout.SOUTH, pinCheckbox);

        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(BorderLayout.CENTER, diagramPanel);
        mainPane.add(BorderLayout.EAST, buttonPane);
        mainPane.setPreferredSize(new Dimension(320, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(_helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }

        // Add an internal frame listsner to VISAT so that we can update our
        // spectrum dialog with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new SpectrumIFL());


        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            registerSelectedProductSceneView(view);
        } else {
            setCurrentProduct(null);
        }

        return mainPane;
    }

    private ProductNodeListenerAdapter createPropertyChangedHandler() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                final String propertyName = event.getPropertyName();
                if (propertyName.equals(DataNode.PROPERTY_NAME_UNIT)) {
                    updateYUnit(getSelectedBands());
                } else if (propertyName.equals(Band.PROPERTY_NAME_SPECTRAL_WAVELENGTH)) {
                    recreateCurrentDiagram(event);
                }
            }

            @Override
            public void nodeAdded(final ProductNodeEvent event) {
                recreateCurrentDiagram(event);
            }

            @Override
            public void nodeRemoved(final ProductNodeEvent event) {
                recreateCurrentDiagram(event);
            }
        };
    }

    private void recreateCurrentDiagram(final ProductNodeEvent event) {
        if (currentProduct != null) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Band) {
                recreateCurrentDiagram();
            }
        }
    }

    private void recreateCurrentDiagram() {
        // Note: this is a brute-force method, but it works :-)
        setCurrentProductDiagram(null);
        final Product currentProduct = this.currentProduct;
        setCurrentProduct(null);
        setCurrentProduct(currentProduct);
    }

    private Diagram getCurrentProductDiagram() {
        Debug.assertNotNull(currentProduct);
        return productToDiagramMap.get(currentProduct);
    }

    private void setCurrentProductDiagram(final Diagram diagram) {
        Debug.assertNotNull(currentProduct);
        if (diagram != null) {
            productToDiagramMap.put(currentProduct, diagram);
        } else {
            productToDiagramMap.remove(currentProduct);
        }
    }

    public void resetAxesMinMaxAccumulators() {
        xMinAuto = +Double.MAX_VALUE;
        xMaxAuto = -Double.MAX_VALUE;
        yMinAuto = +Double.MAX_VALUE;
        yMaxAuto = -Double.MAX_VALUE;
        getContentPane().repaint();
    }


    static class SpectrumGraph implements DiagramGraph {

        private float[] _wavelengths;
        private Band[] _bands;
        private float[] _values;
        private final float[] _ioBuffer;
        private final Range _valueRange;
        private final Range _wavelengthRange;

        public SpectrumGraph(Band[] bands) {
            Debug.assertNotNull(bands);
            _ioBuffer = new float[1];
            _valueRange = new Range();
            _wavelengthRange = new Range();
            setBands(bands);
        }

        public int getNumValues() {
            return _bands.length;
        }

        public String getLabelAt(int index) {
            return getYValueAt(index) + " @ " + getXValueAt(index) + "nm";
        }

        public double getXValueAt(int index) {
            return _wavelengths[index];
        }

        public double getYValueAt(int index) {
            return _values[index];
        }

        public double getXMin() {
            return _wavelengthRange.getMin();
        }

        public double getXMax() {
            return _wavelengthRange.getMax();
        }

        public double getYMin() {
            return _valueRange.getMin();
        }

        public double getYMax() {
            return _valueRange.getMax();
        }

        public Band[] getBands() {
            return _bands;
        }

        public void setBands(Band[] bands) {
            Debug.assertNotNull(bands);
            _bands = bands.clone();
            Arrays.sort(_bands, new Comparator<Band>() {
                public int compare(Band band1, Band band2) {
                    final float v = band1.getSpectralWavelength() - band2.getSpectralWavelength();
                    return v < 0.0F ? -1 : v > 0.0F ? 1 : 0;
                }
            });
            if (_wavelengths == null || _wavelengths.length != _bands.length) {
                _wavelengths = new float[_bands.length];
            }
            if (_values == null || _values.length != _bands.length) {
                _values = new float[_bands.length];
            }
            for (int i = 0; i < _wavelengths.length; i++) {
                _wavelengths[i] = _bands[i].getSpectralWavelength();
                _values[i] = 0f;
            }
            Range.computeRangeFloat(_wavelengths, IndexValidator.TRUE, _wavelengthRange, ProgressMonitor.NULL);
            Range.computeRangeFloat(_values, IndexValidator.TRUE, _valueRange, ProgressMonitor.NULL);
        }

        public void readValues(int pixelX, int pixelY) throws IOException {
            Debug.assertNotNull(_bands);
            for (int i = 0; i < _bands.length; i++) {
                final Band band = _bands[i];
                _values[i] = band.readPixels(pixelX, pixelY, 1, 1, _ioBuffer, ProgressMonitor.NULL)[0];
            }
            Range.computeRangeFloat(_values, IndexValidator.TRUE, _valueRange, ProgressMonitor.NULL);
        }

    }


    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    public void updateComponentTreeUI() {
    }


    private PinSelectionListener getPinSelectionListener() {
        return pinSelectionListener;
    }


    private PixelPositionListener getPixelPositionListener(Product product) {
        assert productToPixelPosListenerMap != null;
        return productToPixelPosListenerMap.get(product);
    }

    private PixelPositionListener registerPixelPositionListener(Product product) {
        if (productToPixelPosListenerMap == null) {
            productToPixelPosListenerMap = new HashMap<Product, PixelPositionListener>();
        }
        PixelPositionListener listener = getPixelPositionListener(product);
        if (listener == null) {
            listener = new SpectrumPPL(product);
            productToPixelPosListenerMap.put(product, listener);
        }
        return listener;
    }

    private boolean isVisible() {
        return getControl().isVisible();
    }

    private void registerSelectedProductSceneView(final ProductSceneView view) {
        final Product product = view.getProduct();
        view.addPixelPositionListener(registerPixelPositionListener(product));
        product.addProductNodeListener(getPinSelectionListener());
        setCurrentProduct(product);
        if (isSnapToPinSelected()) {
            setSpectrumToSelectedPin();
        }
    }

    private void deregisterProductSceneView(final ProductSceneView view) {
        final Product product = view.getProduct();
        view.removePixelPositionListener(getPixelPositionListener(product));
        product.removeProductNodeListener(getPinSelectionListener());
        setCurrentProduct(null);
    }

    private class SpectrumIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                registerSelectedProductSceneView(view);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                deregisterProductSceneView(view);
            }
        }
    }

    private class SpectrumPPL implements PixelPositionListener {

        private final Product _product;

        public SpectrumPPL(Product product) {
            _product = product;
        }

        public Product getProduct() {
            return _product;
        }

        public void pixelPosChanged(RenderedImage sourceImage, int pixelX, int pixelY, boolean pixelPosValid,
                                    MouseEvent e) {
            if (isActive()) {
                setCurrentProduct(getProduct());
                if (pixelPosValid) {
                    updateSpectrum(pixelX, pixelY);
                } else {
                    setInvalidDiagramMessage("Pixel position invalid."); /*I18N*/
                }
            }
            if (e.isShiftDown()) {
                resetAxesMinMaxAccumulators();
            }
        }

        public void pixelPosNotAvailable(RenderedImage sourceImage) {
            if (isActive()) {
                setInvalidDiagramMessage("Pixel position not available."); /*I18N*/
            }
        }

        private boolean isActive() {
            return isVisible() && !isSnapToPinSelected();
        }
    }

    private class PinSelectionListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            if (isActive()) {
                if (!Pin.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                    return;
                }
                updatePin(event);
            }
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeAdded(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeRemoved(ProductNodeEvent event) {
            if (isActive()) {
                ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof Pin && ((Pin) sourceNode).isSelected()) {
                    setNoPinSelected();
                }
            }
        }

        private void updatePin(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin && ((Pin) sourceNode).isSelected()) {
                final Pin pin = ((Pin) sourceNode);
                setSpectrumForPin(pin);
            }
        }

        private boolean isActive() {
            return isVisible() && isSnapToPinSelected();
        }
    }
}
