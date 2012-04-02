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

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.type.*;
import org.opengis.feature.type.GeometryDescriptor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * The scatter plot pane within the statistcs window.
 */
class ScatterPlotPanel extends PagePanel implements SingleRoiComputePanel.ComputeMask {

    private static final String NO_DATA_MESSAGE = "No scatter plot computed yet.\n" +
            ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Scatter Plot";
    private static final String TITLE_PREFIX = CHART_TITLE;

    private static final int X_VAR = 0;
    private static final int Y_VAR = 1;

    private ParamGroup paramGroup;

    private Parameter[] rasterNameParams;
    private Parameter[] autoMinMaxParams;
    private Parameter[] minParams;
    private Parameter[] maxParams;

    private ChartPanel scatterPlotDisplay;
    private boolean adjustingAutoMinMax;
    private XYImagePlot plot;

    private ScatterPlotModel scatterPlotModel;
    private BindingContext bindingContext;

    private Property logScaled;
    private final String PARAM_RASTER_DATA_NODE_NAME = "rasterDataNodeName";
    private final String PARAM_BOX_SIZE = "boxSize";
    private final String PARAM_ROI_MASK_NAME = "roiMaskName";
    private final String PARAM_POINT_DATA_NODE_NAME = "pointDataNodeName";
    private final String PARAM_POINT_DATA_FIELD_NAME = "pointDataFieldName";
    private final String PARAM_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PARAM_Y_AXIS_LOG_SCALED = "yAxisLogScaled";
    private final String PARAM_SHOW_CONFIDENCE_INTERVAL = "showConfidenceInterval";
    private final String PARAM_CONFIDENCE_INTERVAL = "confidenceInterval";

    ScatterPlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId);
    }

    @Override
    public String getTitle() {
        return getTitlePrefix();
    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
        updateContent();
    }

    @Override
    protected void updateContent() {
        if (scatterPlotDisplay != null) {
            plot.setImage(null);
            plot.setDataset(null);
            final String[] availableBands = createAvailableBandList();
            updateParameters(X_VAR, availableBands);
            updateParameters(Y_VAR, availableBands);

            final PropertySet propertySet = bindingContext.getPropertySet();
            propertySet.getProperty(PARAM_RASTER_DATA_NODE_NAME).getDescriptor().setValueSet(new ValueSet(createAvailableBandList()));
            propertySet.getProperty(PARAM_ROI_MASK_NAME).getDescriptor().setValueSet(new ValueSet(getAvailableMaskNames()));
            propertySet.getProperty(PARAM_POINT_DATA_NODE_NAME).getDescriptor().setValueSet(new ValueSet(getAvailablePointDataNodeNames()));

            setChartTitle();
        }
    }

    private String[] getAvailablePointDataNodeNames() {
        final Product product = getProduct();
        if (product != null) {
            final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
            final VectorDataNode[] vectorDataNodes = vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()]);
            final ArrayList<String> names = new ArrayList<String>();
            for (VectorDataNode vectorDataNode : vectorDataNodes) {
                final GeometryDescriptor geometryDescriptor = vectorDataNode.getFeatureType().getGeometryDescriptor();
                final Class<?> geometryBinding = geometryDescriptor.getType().getBinding();
                if (geometryBinding.isAssignableFrom(com.vividsolutions.jts.geom.Point.class)) {
                    names.add(vectorDataNode.getName());
                }
            }
            return names.toArray(new String[names.size()]);
        }
        return new String[0];
    }

    private String[] getAvailableMaskNames() {
        final Product product = getProduct();
        if (product != null) {
            return product.getMaskGroup().getNodeNames();
        }
        return new String[0];
    }

    private void setChartTitle() {
        final JFreeChart chart = scatterPlotDisplay.getChart();
        final List<Title> subtitles = new ArrayList<Title>(7);
        subtitles.add(new TextTitle(MessageFormat.format("{0}, {1}",
                rasterNameParams[X_VAR].getValueAsText(),
                rasterNameParams[Y_VAR].getValueAsText())));
        chart.setSubtitles(subtitles);
    }

    private void updateParameters(int varIndex, String[] availableBands) {

        final RasterDataNode raster = getRaster();
        String rasterName = null;
        if (raster != null) {
            rasterName = raster.getName();
        } else if (availableBands.length > 0) {
            rasterName = availableBands[0];
        }
        if (rasterName != null) {
            rasterNameParams[varIndex].getProperties().setValueSet(availableBands);
            rasterNameParams[varIndex].setValue(rasterName, null);
        } else {
            rasterNameParams[varIndex].getProperties().setValueSet(new String[0]);
        }

        if ((Boolean) autoMinMaxParams[varIndex].getValue()) {
            minParams[varIndex].setDefaultValue();
            maxParams[varIndex].setDefaultValue();
        }
    }

    private void initParameters() {
        scatterPlotModel = new ScatterPlotModel();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(scatterPlotModel));

        rasterNameParams = new Parameter[2];
        autoMinMaxParams = new Parameter[2];
        minParams = new Parameter[2];
        maxParams = new Parameter[2];

        paramGroup = new ParamGroup();

        final PropertyDescriptor logScaleDescriptor = new PropertyDescriptor("scatter.plot.log.scale", Boolean.class);
        logScaleDescriptor.setDisplayName("Log scaled");
        logScaleDescriptor.setDefaultValue(Boolean.FALSE);
        logScaleDescriptor.setDescription("Toggle linear/log10 scaling");

        logScaled = new Property(logScaleDescriptor, new DefaultPropertyAccessor());

        final String[] availableBands = createAvailableBandList();
        initParameters(X_VAR, availableBands);
        initParameters(Y_VAR, availableBands);

        paramGroup.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private void initParameters(int varIndex, String[] availableBands) {

        final String paramPrefix = "var" + varIndex + ".";

        final RasterDataNode raster = getRaster();
        final String rasterName;
        if (raster != null) {
            rasterName = raster.getName();
        } else if (availableBands.length > 0) {
            rasterName = availableBands[0];
        } else {
            rasterName = "";
        }
        rasterNameParams[varIndex] = new Parameter(paramPrefix + "rasterName", rasterName);
        rasterNameParams[varIndex].getProperties().setValueSet(availableBands);
        rasterNameParams[varIndex].getProperties().setValueSetBound(true);
        rasterNameParams[varIndex].getProperties().setDescription("Band name"); /*I18N*/
        rasterNameParams[varIndex].getProperties().setEditorClass(ComboBoxEditor.class);
        paramGroup.addParameter(rasterNameParams[varIndex]);

        autoMinMaxParams[varIndex] = new Parameter(paramPrefix + "autoMinMax", Boolean.TRUE);
        autoMinMaxParams[varIndex].getProperties().setLabel("Auto min/max");
        autoMinMaxParams[varIndex].getProperties().setDescription("Automatically detect min/max");  /*I18N*/
        paramGroup.addParameter(autoMinMaxParams[varIndex]);

        minParams[varIndex] = new Parameter(paramPrefix + "min", 0.0);
        minParams[varIndex].getProperties().setLabel("Min:");
        minParams[varIndex].getProperties().setDescription("Minimum display value");    /*I18N*/
        minParams[varIndex].getProperties().setNumCols(7);
        paramGroup.addParameter(minParams[varIndex]);

        maxParams[varIndex] = new Parameter(paramPrefix + "max", 100.0);
        maxParams[varIndex].getProperties().setLabel("Max:");
        maxParams[varIndex].getProperties().setDescription("Maximum display value");    /*I18N*/
        maxParams[varIndex].getProperties().setNumCols(7);
        paramGroup.addParameter(maxParams[varIndex]);
    }

    private void createUI() {

        final JButton computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(getRaster() != null);
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean maskSelected = isMaskSelected();
                if (maskSelected) {
                    String selectedMaskName = getSelectedMaskName();
                    Mask mask = getRaster().getProduct().getMaskGroup().get(selectedMaskName);
                    compute(mask);
                } else {
                    compute(null);
                }
            }
        });
        computeButton.setIcon(UIUtils.loadImageIcon("icons/Gears20.gif"));


        final JComboBox rasterDataSourceComboBox = new JComboBox();
        bindingContext.bind(PARAM_RASTER_DATA_NODE_NAME, rasterDataSourceComboBox);
        final JPanel rasterDataSourcePanel = new JPanel(new BorderLayout(5, 3));
        rasterDataSourcePanel.add(new JLabel("Raster data source:"), BorderLayout.NORTH);
        rasterDataSourcePanel.add(rasterDataSourceComboBox);


        final JSpinner boxSizeSpinner = new JSpinner();
        bindingContext.bind(PARAM_BOX_SIZE, boxSizeSpinner);
        final JPanel boxSizePanel = new JPanel(new BorderLayout(5, 3));
        boxSizePanel.add(new JLabel("Box size:"), BorderLayout.WEST);
        boxSizePanel.add(boxSizeSpinner);


        final JComboBox roiMaskComboBox = new JComboBox();
        bindingContext.bind(PARAM_ROI_MASK_NAME, roiMaskComboBox);
        final JPanel roiMaskPanel = new JPanel(new BorderLayout(5, 3));
        roiMaskPanel.add(new JLabel("ROI mask:"), BorderLayout.NORTH);
        roiMaskPanel.add(roiMaskComboBox);
        roiMaskPanel.add(createShowMaskManagerButton(), BorderLayout.EAST);

        final JComboBox pointDataSourceCombo = new JComboBox();
        bindingContext.bind(PARAM_POINT_DATA_NODE_NAME, pointDataSourceCombo);
        final JPanel pointDataSourcePanel = new JPanel(new BorderLayout(5, 3));
        pointDataSourcePanel.add(new JLabel("Point data source:"), BorderLayout.NORTH);
        pointDataSourcePanel.add(pointDataSourceCombo);

        final JComboBox pointDataFieldCombo = new JComboBox();
        bindingContext.bind(PARAM_POINT_DATA_FIELD_NAME, pointDataFieldCombo);
        final JPanel pointDataFieldPanel = new JPanel(new BorderLayout(5, 3));
        pointDataFieldPanel.add(new JLabel("Point data field:"), BorderLayout.NORTH);
        pointDataFieldPanel.add(pointDataFieldCombo);

        final AxisRangeControl xAxisRangeControl = new AxisRangeControl("X-Axis");
        final JPanel xAxisRangePanel = xAxisRangeControl.getPanel();

        final JCheckBox xLogCheck = new JCheckBox("Log scaled");
        bindingContext.bind(PARAM_X_AXIS_LOG_SCALED, xLogCheck);
        final JPanel xAxisOptionPanel = new JPanel(new BorderLayout());
        xAxisOptionPanel.add(xAxisRangePanel);
        xAxisOptionPanel.add(xLogCheck, BorderLayout.SOUTH);

        rasterDataSourceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final String value = (String) e.getItem();
                if (value != null) {
                    xAxisRangeControl.setTitleSuffix(value);
                }
            }
        });

        final AxisRangeControl yAxisRangeControl = new AxisRangeControl("Y-Axis");
        final JPanel yAxisRangePanel = yAxisRangeControl.getPanel();

        final JCheckBox yLogCheck = new JCheckBox("Log scaled");
        bindingContext.bind(PARAM_Y_AXIS_LOG_SCALED, yLogCheck);
        final JPanel yAxisOptionPanel = new JPanel(new BorderLayout());
        yAxisOptionPanel.add(yAxisRangePanel);
        yAxisOptionPanel.add(yLogCheck, BorderLayout.SOUTH);

        final JCheckBox confidenceCheck = new JCheckBox("Confidence interval");
        final JTextField confidenceField = new JTextField();
        confidenceField.setPreferredSize(new Dimension(40,confidenceField.getPreferredSize().height));
        confidenceField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PARAM_SHOW_CONFIDENCE_INTERVAL, confidenceCheck);
        bindingContext.bind(PARAM_CONFIDENCE_INTERVAL, confidenceField);
        final JPanel confidencePanel = new JPanel(new BorderLayout(5, 3));
        confidencePanel.add(confidenceCheck, BorderLayout.WEST);
        confidencePanel.add(confidenceField);

        // help

        final JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(getHelpButton(), BorderLayout.EAST);


        // Plot

        plot = new XYImagePlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.getRenderer().setBaseToolTipGenerator(new XYPlotToolTipGenerator());

        JFreeChart chart = new JFreeChart(CHART_TITLE, plot);
        chart.removeLegend();

        scatterPlotDisplay = new ChartPanel(chart);
        scatterPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());

        // UI arrangement

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.top = 6;
        gbc.weighty = 0;
        gbc.gridy = 0;

        final JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.add(computeButton, gbc);
        gbc.gridy++;
        rightPanel.add(rasterDataSourcePanel, gbc);
        gbc.gridy++;
        rightPanel.add(boxSizePanel, gbc);
        gbc.gridy++;
        rightPanel.add(roiMaskPanel, gbc);
        gbc.gridy++;
        rightPanel.add(pointDataSourcePanel, gbc);
        gbc.gridy++;
        rightPanel.add(pointDataFieldPanel, gbc);

        gbc.weighty = 1;
        gbc.gridy++;
        rightPanel.add(new JPanel(), gbc);   // filler

        gbc.weighty = 0;
        gbc.gridy++;
        rightPanel.add(xAxisOptionPanel, gbc);
        gbc.gridy++;
        rightPanel.add(yAxisOptionPanel, gbc);
        gbc.gridy++;
        rightPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridy++;
        rightPanel.add(confidencePanel, gbc);
        gbc.gridy++;
        rightPanel.add(createChartButtonPanel2(scatterPlotDisplay), gbc);

        add(scatterPlotDisplay, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        updateUIState();
    }

    private JButton createShowMaskManagerButton() {
        final JButton showMaskManagerButton = new JButton("...");
        showMaskManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openMaskManager();
            }
        });
        return showMaskManagerButton;
    }

    private void openMaskManager() {
        //Todo change body of created method. Use File | Settings | File Templates to change
    }

    private String getSelectedMaskName() {
        return (String) bindingContext.getBinding(PARAM_ROI_MASK_NAME).getPropertyValue();
    }

    private boolean isMaskSelected() {
        return getSelectedMaskName() != null;
    }

    private RasterDataNode getRaster(int varIndex) {
        final Product product = getProduct();
        if (product == null) {
            return null;
        }
        final String rasterName = rasterNameParams[varIndex].getValue().toString();
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
        updateUIState(X_VAR);
        updateUIState(Y_VAR);
        setChartTitle();
    }

    private void updateUIState(int varIndex) {
        final double min = ((Number) minParams[varIndex].getValue()).doubleValue();
        final double max = ((Number) maxParams[varIndex].getValue()).doubleValue();
        if (!adjustingAutoMinMax && min > max) {
            minParams[varIndex].setValue(max, null);
            maxParams[varIndex].setValue(min, null);
        }
        final boolean autoMinMaxEnabled = (Boolean) autoMinMaxParams[varIndex].getValue();
        minParams[varIndex].setUIEnabled(!autoMinMaxEnabled);
        maxParams[varIndex].setUIEnabled(!autoMinMaxEnabled);
    }

    private static class ScatterPlot {

        private final BufferedImage image;
        private final Range rangeX;
        private final Range rangeY;

        private ScatterPlot(BufferedImage image, Range rangeX, Range rangeY) {
            this.image = image;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
        }

        public BufferedImage getImage() {
            return image;
        }

        public Range getRangeX() {
            return rangeX;
        }

        public Range getRangeY() {
            return rangeY;
        }
    }

    private static class ScatterPlotModel {
        private String rasterDataNodeName;
        private int boxSize;
        private String roiMaskName;
        private String pointDataNodeName;
        private String pointDataFieldName;
        public AxisRangeControl xAxisRangeControl;
        private boolean xAxisLogScaled;
        public AxisRangeControl yAxisRangeControl;
        private boolean yAxisLogScaled;
        private boolean showConfidenceInterval;
        private double confidenceInterval;

    }

    @Override
    public void compute(final Mask selectedMask) {

        final RasterDataNode rasterX = getRaster(X_VAR);
        final RasterDataNode rasterY = getRaster(Y_VAR);

        if (rasterX == null || rasterY == null) {
            return;
        }

        ProgressMonitorSwingWorker<ScatterPlot, Object> swingWorker = new ProgressMonitorSwingWorker<ScatterPlot, Object>(
                this, "Computing scatter plot") {

            @Override
            protected ScatterPlot doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing scatter plot...", 100);
                try {
                    final Range rangeX = getRange(X_VAR, rasterX, selectedMask, SubProgressMonitor.create(pm, 15));
                    final Range rangeY = getRange(Y_VAR, rasterY, selectedMask, SubProgressMonitor.create(pm, 15));
                    final BufferedImage image = ProductUtils.createDensityPlotImage(rasterX,
                            (float) rangeX.getMin(),
                            (float) rangeX.getMax(),
                            rasterY,
                            (float) rangeY.getMin(),
                            (float) rangeY.getMax(),
                            selectedMask,
                            512,
                            512,
                            new Color(255, 255, 255, 0),
                            null,
                            SubProgressMonitor.create(pm, 70));
                    return new ScatterPlot(image, rangeX, rangeY);
                } finally {
                    pm.done();
                }
            }

            @Override
            public void done() {
                try {
                    final ScatterPlot scatterPlot = get();
                    double minX = scatterPlot.getRangeX().getMin();
                    double maxX = scatterPlot.getRangeX().getMax();
                    double minY = scatterPlot.getRangeY().getMin();
                    double maxY = scatterPlot.getRangeY().getMax();
                    if (minX > maxX || minY > maxY) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                "Failed to compute scatter plot.\n" +
                                        "No Pixels considered..",
                                /*I18N*/
                                CHART_TITLE, /*I18N*/
                                JOptionPane.ERROR_MESSAGE);
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
                    plot.setImage(scatterPlot.getImage());
                    plot.setImageDataBounds(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
                    setAutoRange(X_VAR, new Range(minX, maxX));
                    setAutoRange(Y_VAR, new Range(minY, maxY));
                    plot.getDomainAxis().setLabel(getAxisLabel(getRaster(X_VAR)));
                    plot.getRangeAxis().setLabel(getAxisLabel(getRaster(Y_VAR)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute scatter plot.\n" +
                                    "Calculation canceled.",
                            /*I18N*/
                            CHART_TITLE, /*I18N*/
                            JOptionPane.ERROR_MESSAGE);
                } catch (CancellationException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute scatter plot.\n" +
                                    "Calculation canceled.",
                            /*I18N*/
                            CHART_TITLE, /*I18N*/
                            JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute scatter plot.\n" +
                                    "An error occured:\n" +
                                    e.getCause().getMessage(),
                            CHART_TITLE, /*I18N*/
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        swingWorker.execute();
    }

    private String getAxisLabel(RasterDataNode raster) {
        final String unit = raster.getUnit();
        if (unit != null) {
            return raster.getName() + " (" + unit + ")";
        }
        return raster.getName();
    }

    private Range getRange(int varIndex, RasterDataNode raster, Mask mask, ProgressMonitor pm) throws
            IOException {
        final boolean autoMinMax = (Boolean) autoMinMaxParams[varIndex].getValue();
        if (autoMinMax) {
            Stx stx;
            if (mask == null) {
                stx = raster.getStx(false, pm);
            } else {
                stx = new StxFactory().withRoiMask(mask).create(raster, pm);
            }
            return new Range(stx.getMinimum(), stx.getMaximum());
        } else {
            return new Range((Double) minParams[varIndex].getValue(), (Double) maxParams[varIndex].getValue());
        }
    }

    private void setAutoRange(int varIndex, Range range) {
        final boolean autoMinMax = (Boolean) autoMinMaxParams[varIndex].getValue();
        if (autoMinMax) {
            adjustingAutoMinMax = true;
            minParams[varIndex].setValueAsText(String.format("%7.2f", range.getMin()), null);
            maxParams[varIndex].setValueAsText(String.format("%7.2f", range.getMax()), null);
            adjustingAutoMinMax = false;
        }
    }

    private String[] createAvailableBandList() {
        final Product product = getProduct();
        final List<String> availableBandList = new ArrayList<String>(17);
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                availableBandList.add(band.getName());
            }
            for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                final TiePointGrid grid = product.getTiePointGridAt(i);
                availableBandList.add(grid.getName());
            }
        }
        // if raster is only binded to the product and does not belong to it
        final RasterDataNode raster = getRaster();
        if (raster != null && raster.getProduct() == product) {
            final String rasterName = raster.getName();
            if (!availableBandList.contains(rasterName)) {
                availableBandList.add(rasterName);
            }
        }

        availableBandList.add(0, "");
        return availableBandList.toArray(new String[availableBandList.size()]);
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
                    "This scatter plot diagram contains {0} non-empty bins.\n" +
                            "For each bin, a text data row containing an x, y and z value will be created.\n" +
                            "{1}\nPress ''Yes'' if you really want to copy this amount of data to the system clipboard.\n",
                    numNonEmptyBins, excelNote);
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

        final StringBuffer sb = new StringBuffer(64000);
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
}

