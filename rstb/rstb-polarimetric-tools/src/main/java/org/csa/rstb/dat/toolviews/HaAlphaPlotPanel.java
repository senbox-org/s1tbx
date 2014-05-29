/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.csa.rstb.dat.toolviews;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.csa.rstb.gpf.HaAlphaDescriptor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.toolviews.stat.*;
import org.esa.nest.dat.utils.Palette;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * The H-a Alpha plot pane within the statistics window.
 */
public class HaAlphaPlotPanel extends ChartPagePanel {

    private static final String NO_DATA_MESSAGE = "This plot requires an H-a Alpha decomposition as input\n" +
            "The plot will be computed when you hit the 'Refresh View' button.\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "H-Alpha Plane Plot";

    public final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    public final static String PROPERTY_NAME_MIN = "min";
    public final static String PROPERTY_NAME_MAX = "max";
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_ROI_MASK = "roiMask";
    public final static String PROPERTY_NAME_X_BAND = "xBand";
    public final static String PROPERTY_NAME_Y_BAND = "yBand";

    private static final int X_VAR = 0;
    private static final int Y_VAR = 1;

    private static final int NUM_DECIMALS = 2;

    private BindingContext bindingContext;
    private DataSourceConfig dataSourceConfig;
    private Property xBandProperty;
    private Property yBandProperty;

    private static AxisRangeControl[] axisRangeControls = new AxisRangeControl[2];
    private IndexColorModel toggledColorModel;
    private IndexColorModel untoggledColorModel;

    private ChartPanel densityPlotDisplay;
    private XYImagePlot plot;
    private static final Color backgroundColor = new Color(255, 255, 255, 0);
    private boolean plotColorsInverted;
    private JCheckBox toggleZoneOverlayCheckBox;

    private final static Color annotColour =  Color.DARK_GRAY;
    private final static Font annotFont = new Font("Ariel", Font.BOLD, 14);

    public HaAlphaPlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE, true);
    }

    @Override
    protected void initComponents() {
        initParameters();
        createUI();
        initActionEnablers();
        updateComponents();
    }

    private void initActionEnablers() {
        RefreshActionEnabler roiMaskActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_USE_ROI_MASK,
                PROPERTY_NAME_ROI_MASK, PROPERTY_NAME_X_BAND, PROPERTY_NAME_Y_BAND);
        bindingContext.addPropertyChangeListener(roiMaskActionEnabler);
        RefreshActionEnabler rangeControlActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_MIN, PROPERTY_NAME_AUTO_MIN_MAX,
                PROPERTY_NAME_MAX);
        axisRangeControls[X_VAR].getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        axisRangeControls[Y_VAR].getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        super.nodeDataChanged(event);
        if (!dataSourceConfig.useRoiMask) {
            return;
        }
        final Mask roiMask = dataSourceConfig.roiMask;
        if (roiMask == null) {
            return;
        }
        final ProductNode sourceNode = event.getSourceNode();
        if (!(sourceNode instanceof Mask)) {
            return;
        }
        final String maskName = sourceNode.getName();
        if (roiMask.getName().equals(maskName)) {
            updateComponents();
        }
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        if (isRasterChanged() || isProductChanged()) {
            plot.setImage(null);
            plot.setDataset(null);
            if (isProductChanged()) {
                plot.getDomainAxis().setLabel("Entropy");
                plot.getRangeAxis().setLabel("Alpha");
            }
            final Product product = getProduct();
            if (product != null) {

                toggleZoneOverlayCheckBox.setEnabled(false);
                Band entropyBand = product.getBand("Entropy");
                Band alphaBand = product.getBand("Alpha");
                if (entropyBand != null && alphaBand != null) {
                    try {
                        xBandProperty.setValue(entropyBand);
                        yBandProperty.setValue(alphaBand);
                    } catch (ValidationException ignored) {
                        Debug.trace(ignored);
                    }
                }
            }
        }
        refreshButton.setEnabled(xBandProperty.getValue() != null && yBandProperty.getValue() != null);
    }

    private void initParameters() {
        axisRangeControls[X_VAR] = new AxisRangeControl("X-Axis");
        axisRangeControls[Y_VAR] = new AxisRangeControl("Y-Axis");
        initColorModels();
        plotColorsInverted = false;
        dataSourceConfig = new DataSourceConfig();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        xBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_X_BAND);
        yBandProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_Y_BAND);
    }

    private void initColorModels() {
        for (int j = 0; j <= 1; j++) {
            final int palSize = 256;
            final byte[] r = new byte[palSize];
            final byte[] g = new byte[palSize];
            final byte[] b = new byte[palSize];
            final byte[] a = new byte[palSize];
            r[0] = (byte) backgroundColor.getRed();
            g[0] = (byte) backgroundColor.getGreen();
            b[0] = (byte) backgroundColor.getBlue();
            a[0] = (byte) backgroundColor.getAlpha();

            final Palette pal = new Palette("Rainbow", new Color[]
                    {Color.black, Color.blue, Color.cyan, Color.green, Color.yellow, Color.orange, Color.red});
            for (int i = 1; i < 256; ++i) {
                float value = i / 255f;
                if (j == 0)
                    value = (255 - i) / 255f;
                Color c = pal.lookupColor(value);
                r[i] = (byte) c.getRed();
                g[i] = (byte) c.getGreen();
                b[i] = (byte) c.getBlue();
                a[i] = (byte) 255;
            }

            if (j == 0) {
                toggledColorModel = new IndexColorModel(8, palSize, r, g, b, a);
            } else {
                untoggledColorModel = new IndexColorModel(8, palSize, r, g, b, a);
            }
        }
    }

    private void createUI() {
        plot = new XYImagePlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setAutoRangeIncludesZero(false);
        domainAxis.setUpperMargin(0);
        domainAxis.setLowerMargin(0);
        rangeAxis.setUpperMargin(0);
        rangeAxis.setLowerMargin(0);
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.getRenderer().setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        JFreeChart chart = new JFreeChart(CHART_TITLE, plot);
        ChartFactory.getChartTheme().apply(chart);

        chart.removeLegend();
        createUI(createChartPanel(chart), createOptionsPanel(), bindingContext);
        updateUIState();
    }

    private void toggleColor() {
        BufferedImage image = plot.getImage();
        if (image != null) {
            if (!plotColorsInverted) {
                image = new BufferedImage(untoggledColorModel, image.getRaster(), image.isAlphaPremultiplied(), null);
            } else {
                image = new BufferedImage(toggledColorModel, image.getRaster(), image.isAlphaPremultiplied(), null);
            }
            plot.setImage(image);
            densityPlotDisplay.getChart().setNotify(true);
            plotColorsInverted = !plotColorsInverted;
        }
    }

    private JPanel createOptionsPanel() {
        toggleZoneOverlayCheckBox = new JCheckBox("Show Zones");
        toggleZoneOverlayCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateChartData();
            }
        });
        toggleZoneOverlayCheckBox.setEnabled(false);
        toggleZoneOverlayCheckBox.setSelected(true);
        final JPanel optionsPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=0,weightx=1,gridx=0");
        GridBagUtils.addToPanel(optionsPanel, axisRangeControls[X_VAR].getPanel(), gbc, "gridy=0");
        GridBagUtils.addToPanel(optionsPanel, axisRangeControls[Y_VAR].getPanel(), gbc, "gridy=2,insets.left=0,insets.right=0");
        GridBagUtils.addToPanel(optionsPanel, new JPanel(), gbc, "gridy=4");
        GridBagUtils.addToPanel(optionsPanel, new JSeparator(), gbc, "gridy=5,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(optionsPanel, toggleZoneOverlayCheckBox, gbc, "gridy=6,insets.left=0,insets.right=0");

        GridBagUtils.addToPanel(optionsPanel, new JLabel("Zones Descriptions:"), gbc, "gridy=8,insets.left=0,insets.right=0");
        JTextArea textArea = new JTextArea(
                "Z1 - Dihedral Reflector\n" +
                "Z2 - Dipole\n" +
                "Z3 - Bragg Surface\n" +
                "Z4 - Double Reflection\n" +
                "Z5 - Anisotropic Particles\n" +
                "Z6 - Random Surface\n" +
                "Z7 - Complex Structures\n" +
                "Z8 - Random Anisotropic Scatterers\n" +
                "Z9 - Non-feasible");
        GridBagUtils.addToPanel(optionsPanel, textArea, gbc, "gridy=9,insets.left=0,insets.right=0");
        return optionsPanel;
    }

    private ChartPanel createChartPanel(JFreeChart chart) {
        densityPlotDisplay = new ChartPanel(chart);

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                densityPlotDisplay,
                "scatter_plot_area",
                "Mask generated from selected scatter plot area",
                Color.RED,
                PlotAreaSelectionTool.AreaType.ELLIPSE) {
            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getCenterX(), bounds.getCenterY(), 0.5 * bounds.getWidth(), 0.5 * bounds.getHeight());
            }

            protected String createMaskExpression(double x0, double y0, double dx, double dy) {
                return String.format("sqrt(sqr((%s - %s)/%s) + sqr((%s - %s)/%s)) < 1.0",
                        BandArithmetic.createExternalName(dataSourceConfig.xBand.getName()),
                        x0,
                        dx,
                        BandArithmetic.createExternalName(dataSourceConfig.yBand.getName()),
                        y0,
                        dy);
            }
        };

        densityPlotDisplay.getPopupMenu().addSeparator();
        densityPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        densityPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        densityPlotDisplay.getPopupMenu().addSeparator();
        densityPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return densityPlotDisplay;
    }

    private RasterDataNode getRaster(int varIndex) {
        final Product product = getProduct();
        if (product == null) {
            return null;
        }
        final String rasterName;// = rasterNameParams[varIndex].getValue().toString();
        if (varIndex == X_VAR) {
            rasterName = dataSourceConfig.xBand.getName();
        } else {
            rasterName = dataSourceConfig.yBand.getName();
        }
        RasterDataNode raster = product.getRasterDataNode(rasterName);
        if (raster == null) {
            if (getRaster() != null && rasterName.equalsIgnoreCase(getRaster().getName())) {
                raster = getRaster();
            }
        }
        Debug.assertTrue(raster != null);
        return raster;
    }

    private void updateUIState() {
        super.updateComponents();
    }

    private void checkBandsForRange() throws IllegalArgumentException {
        if (axisRangeControls[X_VAR].getMin().equals(axisRangeControls[X_VAR].getMax()) &&
                axisRangeControls[Y_VAR].getMin().equals(axisRangeControls[Y_VAR].getMax())) {
            throw new IllegalArgumentException("Value range of at least one band must be larger than one");
        }
    }

    @Override
    protected void updateChartData() {

        final RasterDataNode rasterX = getRaster(X_VAR);
        final RasterDataNode rasterY = getRaster(Y_VAR);

        if (rasterX == null || rasterY == null) {
            return;
        }

        ProgressMonitorSwingWorker<BufferedImage, Object> swingWorker = new ProgressMonitorSwingWorker<BufferedImage, Object>(
                this, "Computing plot") {

            @Override
            protected BufferedImage doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing plot...", 100);
                try {
                    checkBandsForRange();
                    setRange(X_VAR, rasterX, dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null, SubProgressMonitor.create(pm, 15));
                    setRange(Y_VAR, rasterY, dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null, SubProgressMonitor.create(pm, 15));
                    BufferedImage densityPlotImage = ProductUtils.createDensityPlotImage(rasterX,
                            axisRangeControls[X_VAR].getMin().floatValue(),
                            axisRangeControls[X_VAR].getMax().floatValue(),
                            rasterY,
                            axisRangeControls[Y_VAR].getMin().floatValue(),
                            axisRangeControls[Y_VAR].getMax().floatValue(),
                            dataSourceConfig.useRoiMask ? dataSourceConfig.roiMask : null,
                            512,
                            512,
                            backgroundColor,
                            null,
                            SubProgressMonitor.create(pm, 70));

                    densityPlotImage = new BufferedImage(untoggledColorModel, densityPlotImage.getRaster(), densityPlotImage.isAlphaPremultiplied(), null);

                    plotColorsInverted = false;
                    return densityPlotImage;
                } finally {
                    pm.done();
                }
            }

            @Override
            public void done() {
                try {
                    checkBandsForRange();
                    final BufferedImage densityPlotImage = get();
                    double minX = axisRangeControls[X_VAR].getMin();
                    double maxX = axisRangeControls[X_VAR].getMax();
                    double minY = axisRangeControls[Y_VAR].getMin();
                    double maxY = axisRangeControls[Y_VAR].getMax();
                    if (minX > maxX || minY > maxY) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                "Failed to compute plot.\n" +
                                        "No Pixels considered..",
                                /*I18N*/
                                CHART_TITLE, /*I18N*/
                                JOptionPane.ERROR_MESSAGE
                        );
                        plot.setDataset(null);
                        return;

                    }

                    if (MathUtils.equalValues(minX, maxX, 1.0e-4)) {
                        minX = Math.floor(minX);
                        maxX = Math.ceil(maxX);
                    }
                    if (MathUtils.equalValues(minY, maxY, 1.0e-4)) {
                        minY = Math.floor(minY);
                        maxY = Math.ceil(maxY);
                    }
                    plot.setImage(densityPlotImage);
                    plot.setImageDataBounds(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
                    axisRangeControls[X_VAR].adjustComponents(minX, maxX, NUM_DECIMALS);
                    axisRangeControls[Y_VAR].adjustComponents(minY, maxY, NUM_DECIMALS);
//                    plot.getDomainAxis().setLabel(StatisticChartStyling.getAxisLabel(getRaster(X_VAR), "Entropy", false));
//                    plot.getRangeAxis().setLabel(StatisticChartStyling.getAxisLabel(getRaster(Y_VAR), "Alpha", false));
                    plot.getDomainAxis().setLabel("Entropy");
                    plot.getRangeAxis().setLabel("Alpha");
                    toggleZoneOverlayCheckBox.setEnabled(true);


                    // clear the list
                    java.util.List<XYAnnotation> annotList = plot.getAnnotations();
                    for(XYAnnotation an : annotList) {
                        plot.removeAnnotation(an);
                    }

                    if(toggleZoneOverlayCheckBox.isSelected()) {
                        drawZoneOverlay();
                    }

                } catch (InterruptedException | CancellationException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute plot.\n" +
                                    "Calculation canceled.",
                            /*I18N*/
                            CHART_TITLE, /*I18N*/
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (ExecutionException | IllegalArgumentException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute plot.\n" +
                                    "An error occurred:\n" +
                                    e.getCause().getMessage(),
                            CHART_TITLE, /*I18N*/
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        swingWorker.execute();
    }

    private void drawZoneOverlay() {
        double minX = axisRangeControls[X_VAR].getMin();
        double maxX = axisRangeControls[X_VAR].getMax();
        double minY = axisRangeControls[Y_VAR].getMin();
        double maxY = axisRangeControls[Y_VAR].getMax();
        double H1 = HaAlphaDescriptor.H1;
        double H2 = HaAlphaDescriptor.H2;
        double Alpha1 = HaAlphaDescriptor.Alpha1;
        double Alpha2 = HaAlphaDescriptor.Alpha2;
        double Alpha3 = HaAlphaDescriptor.Alpha3;
        double Alpha4 = HaAlphaDescriptor.Alpha4;
        double Alpha5 = HaAlphaDescriptor.Alpha5;

        BasicStroke stroke = new BasicStroke(2.0f);

        XYLineAnnotation line = new XYLineAnnotation(H1, minY, H1, maxY, stroke, annotColour);
        plot.addAnnotation(line);
        line = new XYLineAnnotation(H2, minY, H2, maxY, stroke, annotColour);
        plot.addAnnotation(line);

        line = new XYLineAnnotation(H1, Alpha1, maxX, Alpha1, stroke, annotColour);
        plot.addAnnotation(line);
        line = new XYLineAnnotation(H2, Alpha2, H1, Alpha2, stroke, annotColour);
        plot.addAnnotation(line);
        line = new XYLineAnnotation(minX, Alpha3, H2, Alpha3, stroke, annotColour);
        plot.addAnnotation(line);
        line = new XYLineAnnotation(minX, Alpha4, H2, Alpha4, stroke, annotColour);
        plot.addAnnotation(line);
        line = new XYLineAnnotation(H2, Alpha5, maxX, Alpha5, stroke, annotColour);
        plot.addAnnotation(line);

        addText("Z1", minX+(H2-minX)/2, Alpha3+(maxY-Alpha3)/2);
        addText("Z2", minX+(H2-minX)/2, Alpha4+(Alpha3-Alpha4)/2);
        addText("Z3", minX+(H2-minX)/2, minY+(Alpha4-minY)/2);

        addText("Z4", H2+(H1-H2)/2, Alpha2+(maxY-Alpha2)/2);
        addText("Z5", H2+(H1-H2)/2, Alpha5+(Alpha2-Alpha5)/2);
        addText("Z6", H2+(H1-H2)/2, minY+(Alpha5-minY)/2);

        addText("Z7", H1+(maxX-H1)/2, Alpha1+(maxY-Alpha1)/2);
        addText("Z8", H1+(maxX-H1)/2, Alpha5+(Alpha1-Alpha5)/2);
        addText("Z9", H1+(maxX-H1)/2, minY+(Alpha5-minY)/2);

        //Arc2D.Double arc = new Arc2D.Double(
        //        0, 0, 30, 2 * 30, 3.14, 3.14, Arc2D.OPEN);
        //plot.addAnnotation(new XYShapeAnnotation(arc,
        //        new BasicStroke(2.0f), Color.blue));
    }

    private void addText(final String text, final double x, final double y) {
        final XYTextAnnotation annotation = new XYTextAnnotation(text, x, y);
        annotation.setPaint(annotColour);
        annotation.setBackgroundPaint(Color.WHITE);
        annotation.setFont(annotFont);
        plot.addAnnotation(annotation);
    }

    private void setRange(int varIndex, RasterDataNode raster, Mask mask, ProgressMonitor pm) throws IOException {
        final AxisRangeControl axisRangeControl = axisRangeControls[varIndex];
        if(varIndex == X_VAR) {
            axisRangeControl.adjustComponents(0.0, 1.0, NUM_DECIMALS);
            return;
        }
        if (axisRangeControl.isAutoMinMax()) {
            Stx stx;
            if (mask == null) {
                stx = raster.getStx(false, pm);
            } else {
                stx = new StxFactory().withRoiMask(mask).create(raster, pm);
            }
            axisRangeControl.adjustComponents(Math.min(0, stx.getMinimum()), Math.max(90, stx.getMaximum()), NUM_DECIMALS);
        }
    }

    @Override
    protected boolean checkDataToClipboardCopy() {
        final int warnLimit = 2000;
        final int excelLimit = 65536;
        final int numNonEmptyBins = getNumNonEmptyBins();
        if (numNonEmptyBins > warnLimit) {
            String excelNote = "";
            if (numNonEmptyBins > excelLimit - 100) {
                excelNote = "Note that e.g., Microsoft® Excel 2002 only supports a total of "
                        + excelLimit + " rows in a sheet.\n";   /*I18N*/
            }
            final String message = MessageFormat.format(
                    "This scatter plot contains {0} non-empty bins.\n" +
                            "For each bin, a text data row containing an x, y and z value will be created.\n" +
                            "{1}\nPress ''Yes'' if you really want to copy this amount of data to the system clipboard.\n",
                    numNonEmptyBins, excelNote
            );
            final int status = JOptionPane.showConfirmDialog(this,
                    message, /*I18N*/
                    "Copy Data to Clipboard", /*I18N*/
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (status != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return true;
    }

    private byte[] getValidData(BufferedImage image) {
        if (image != null &&
                image.getColorModel() instanceof IndexColorModel &&
                image.getData().getDataBuffer() instanceof DataBufferByte) {
            return ((DataBufferByte) image.getData().getDataBuffer()).getData();
        }
        return null;
    }

    protected int getNumNonEmptyBins() {
        final byte[] data = getValidData(plot.getImage());
        int n = 0;
        if (data != null) {
            int b;
            for (byte aData : data) {
                b = aData & 0xff;
                if (b != 0) {
                    n++;
                }
            }
        }
        return n;
    }

    @Override
    protected String getDataAsText() {
        final BufferedImage image = plot.getImage();
        final Rectangle2D bounds = plot.getImageDataBounds();

        final byte[] data = getValidData(image);
        if (data == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(64000);
        final int w = image.getWidth();
        final int h = image.getHeight();

        final RasterDataNode rasterX = getRaster(X_VAR);
        final String nameX = rasterX.getName();
        final double sampleMinX = bounds.getMinX();
        final double sampleMaxX = bounds.getMaxX();

        final RasterDataNode rasterY = getRaster(Y_VAR);
        final String nameY = rasterY.getName();
        final double sampleMinY = bounds.getMinY();
        final double sampleMaxY = bounds.getMaxY();

        sb.append("Product name:\t").append(rasterX.getProduct().getName()).append("\n");
        sb.append("Dataset X name:\t").append(nameX).append("\n");
        sb.append("Dataset Y name:\t").append(nameY).append("\n");
        sb.append('\n');
        sb.append(nameX).append(" minimum:\t").append(sampleMinX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" maximum:\t").append(sampleMaxX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" bin size:\t").append((sampleMaxX - sampleMinX) / w).append("\t").append(
                rasterX.getUnit()).append("\n");
        sb.append(nameX).append(" #bins:\t").append(w).append("\n");
        sb.append('\n');
        sb.append(nameY).append(" minimum:\t").append(sampleMinY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" maximum:\t").append(sampleMaxY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" bin size:\t").append((sampleMaxY - sampleMinY) / h).append("\t").append(
                rasterY.getUnit()).append("\n");
        sb.append(nameY).append(" #bins:\t").append(h).append("\n");
        sb.append('\n');

        sb.append(nameX);
        sb.append('\t');
        sb.append(nameY);
        sb.append('\t');
        sb.append("Bin counts\t(cropped at 255)");
        sb.append('\n');

        int x, y, z;
        double v1, v2;
        for (int i = 0; i < data.length; i++) {
            z = data[i] & 0xff;
            if (z != 0) {

                x = i % w;
                y = h - i / w - 1;

                v1 = sampleMinX + ((x + 0.5) * (sampleMaxX - sampleMinX)) / w;
                v2 = sampleMinY + ((y + 0.5) * (sampleMaxY - sampleMinY)) / h;

                sb.append(v1);
                sb.append('\t');
                sb.append(v2);
                sb.append('\t');
                sb.append(z);
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private static class DataSourceConfig {
        public boolean useRoiMask;
        public Mask roiMask;
        private RasterDataNode xBand;
        private RasterDataNode yBand;
        private Property xBandProperty;
        private Property yBandProperty;
    }

}

