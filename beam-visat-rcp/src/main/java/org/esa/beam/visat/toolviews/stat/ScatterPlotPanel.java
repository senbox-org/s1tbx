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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.math.Range;
import org.geotools.feature.FeatureCollection;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.SeriesDataset;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

    private ChartPanel scatterPlotDisplay;
    private boolean adjustingAutoMinMax;
    private XYPlot plot;

    private ScatterPlotModel scatterPlotModel;
    private BindingContext bindingContext;

    private Property logScaled;
    private final String PARAM_RASTER_DATA_NODE_NAME = "rasterDataNodeName";
    private final String PARAM_BOX_SIZE = "boxSize";
    private final String PARAM_ROI_MASK_NAME = "roiMaskName";
    private final String PARAM_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PARAM_Y_AXIS_LOG_SCALED = "yAxisLogScaled";
    private final String PARAM_SHOW_CONFIDENCE_INTERVAL = "showConfidenceInterval";
    private final String PARAM_CONFIDENCE_INTERVAL = "confidenceInterval";

    private RoiMaskSelector roiMaskSelector;
    private CorrelativeFieldSelector correlativeFieldSelector;
    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;

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
            plot.setDataset(null);

            final Product product = getProduct();
            roiMaskSelector.updateMaskSource(product);
            correlativeFieldSelector.updatePointDataSource(product);

            setChartTitle();
        }
    }

    private void setChartTitle() {
        final String xAxisName;
        if (getRaster() != null) {
            xAxisName = getAxisLabel(getRaster());
        } else {
            xAxisName = "<none>";
        }

        final String yAxisName;
        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (dataField != null) {
            yAxisName = dataField.getLocalName();
        } else {
            yAxisName = "<none>";
        }

        final JFreeChart chart = scatterPlotDisplay.getChart();
        final List<Title> subtitles = new ArrayList<Title>(7);
        subtitles.add(new TextTitle(MessageFormat.format("{0}, {1}",
                xAxisName,
                yAxisName
        )));
        chart.setSubtitles(subtitles);
    }

    private void initParameters() {
        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");
        scatterPlotModel = new ScatterPlotModel();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(scatterPlotModel));

        bindingContext.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateUIState();
            }
        });

        bindingContext.addPropertyChangeListener(PARAM_X_AXIS_LOG_SCALED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateScalingOfXAxis();
            }
        });
        bindingContext.addPropertyChangeListener(PARAM_Y_AXIS_LOG_SCALED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateScalingOfYAxis();
            }
        });
    }

    @Override
    protected void setRaster(RasterDataNode raster) {
        super.setRaster(raster);
        xAxisRangeControl.setTitleSuffix(raster != null ? raster.getName() : null);
        updateUIState();
    }

    private void createUI() {

        final JSpinner boxSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 2));
        bindingContext.bind(PARAM_BOX_SIZE, boxSizeSpinner);
        final JPanel boxSizePanel = new JPanel(new BorderLayout(5, 3));
        boxSizePanel.add(new JLabel("Box size:"), BorderLayout.WEST);
        boxSizePanel.add(boxSizeSpinner);

        roiMaskSelector = new RoiMaskSelector(bindingContext);
        final JPanel roiMaskPanel = roiMaskSelector.createPanel();

        correlativeFieldSelector = new CorrelativeFieldSelector(bindingContext);

        final JPanel pointDataSourcePanel = new JPanel(new BorderLayout(5, 3));
        pointDataSourcePanel.add(correlativeFieldSelector.pointDataSourceLabel, BorderLayout.NORTH);
        pointDataSourcePanel.add(correlativeFieldSelector.pointDataSourceList);

        final JPanel pointDataFieldPanel = new JPanel(new BorderLayout(5, 3));
        pointDataFieldPanel.add(correlativeFieldSelector.dataFieldLabel, BorderLayout.NORTH);
        pointDataFieldPanel.add(correlativeFieldSelector.dataFieldList);

        final JCheckBox xLogCheck = new JCheckBox("Log scaled");
        bindingContext.bind(PARAM_X_AXIS_LOG_SCALED, xLogCheck);
        final JPanel xAxisOptionPanel = new JPanel(new BorderLayout());
        xAxisOptionPanel.add(xAxisRangeControl.getPanel());
        xAxisOptionPanel.add(xLogCheck, BorderLayout.SOUTH);

        final JCheckBox yLogCheck = new JCheckBox("Log scaled");
        bindingContext.bind(PARAM_Y_AXIS_LOG_SCALED, yLogCheck);
        final JPanel yAxisOptionPanel = new JPanel(new BorderLayout());
        yAxisOptionPanel.add(yAxisRangeControl.getPanel());
        yAxisOptionPanel.add(yLogCheck, BorderLayout.SOUTH);

        final JCheckBox confidenceCheck = new JCheckBox("Confidence interval");
        final JTextField confidenceField = new JTextField();
        confidenceField.setPreferredSize(new Dimension(40, confidenceField.getPreferredSize().height));
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

        plot = new XYPlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setRenderer(new XYLineAndShapeRenderer(false, true));
        plot.getRenderer().setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        final DeviationRenderer deviationRenderer = new DeviationRenderer(true, false);
        deviationRenderer.setBasePaint(Color.black);
        deviationRenderer.setBaseFillPaint(Color.lightGray);
        plot.setRenderer(1, deviationRenderer);
        plot.setDomainAxis(createNumberAxis());
        plot.setRangeAxis(createNumberAxis());

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

    private NumberAxis createNumberAxis() {
        final NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        return rangeAxis;
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

    private Mask getSelectedMask() {

        return scatterPlotModel.roiMask;
    }

    private boolean isMaskSelected() {
        return getSelectedMask() != null;
    }

    private void updateUIState() {
        setChartTitle();
        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        yAxisRangeControl.setTitleSuffix(dataField != null ? dataField.getLocalName() : null);

        if (canCompute()) {
            compute(getSelectedMask());

        }
    }

    private boolean canCompute() {
        return scatterPlotModel.dataField != null && getRaster() != null;
    }

    private static class ScatterPlot {

        private final XYIntervalSeries xySeries;
        private final Range rasterRange;
        private final Range insituRange;

        private ScatterPlot(XYIntervalSeries xySeries, Range rasterRange, Range insituRange) {
            this.xySeries = xySeries;
            this.rasterRange = rasterRange;
            this.insituRange = insituRange;
        }

        public XYIntervalSeries getXySeries() {
            return xySeries;
        }

        public Range getRasterRange() {
            return rasterRange;
        }

        public Range getInsituRange() {
            return insituRange;
        }
    }

    private static class ScatterPlotModel {
        private int boxSize;
        private Boolean useRoiMask;
        private Mask roiMask;
        private VectorDataNode pointDataSource;
        private AttributeDescriptor dataField;
        private boolean xAxisLogScaled;
        private boolean yAxisLogScaled;
        private boolean showConfidenceInterval;
        private double confidenceInterval;

    }

    @Override
    public void compute(final Mask selectedMask) {

        final RasterDataNode raster = getRaster();

        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (raster == null || dataField == null) {
            return;
        }

        ProgressMonitorSwingWorker<ScatterPlot, Object> swingWorker = new ProgressMonitorSwingWorker<ScatterPlot, Object>(
                this, "Computing scatter plot") {

            @Override
            protected ScatterPlot doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing scatter plot...", 100);
                try {
                    final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = scatterPlotModel.pointDataSource.getFeatureCollection();
                    final SimpleFeature[] features = collection.toArray(new SimpleFeature[collection.size()]);

                    double minRaster = Double.MAX_VALUE;
                    double maxRaster = Double.MIN_VALUE;
                    double minInsitu = Double.MAX_VALUE;
                    double maxInsitu = Double.MIN_VALUE;

                    final XYIntervalSeries scatterValues = new XYIntervalSeries("scatter values");
                    for (SimpleFeature feature : features) {
                        final com.vividsolutions.jts.geom.Point point;
                        point = (com.vividsolutions.jts.geom.Point) feature.getDefaultGeometryProperty().getValue();
                        final int pixelX = (int) point.getX();
                        final int pixelY = (int) point.getY();
//                        final int boxSize = scatterPlotModel.boxSize;
//                        final double[] rasterValues = new double[boxSize * boxSize];
                        final double[] rasterValues = new double[1];
                        if (selectedMask == null || selectedMask.isPixelValid(pixelX, pixelY)) {
//                            todo  use BoxSize
                            final double rasterValue = raster.readPixels(pixelX, pixelY, 1, 1, rasterValues)[0];
                            minRaster = Math.min(minRaster, rasterValue);
                            maxRaster = Math.max(maxRaster, rasterValue);
                            final double insituValue = ((Number) feature.getAttribute(dataField.getLocalName())).doubleValue();
                            minInsitu = Math.min(minInsitu, insituValue);
                            maxInsitu = Math.max(maxInsitu, insituValue);
                            final double x = rasterValue;
                            final double y = insituValue;
                            scatterValues.add(x, x, x, y, y, y);
                        }
                    }


                    final Range rasterRange = new Range(minRaster, maxRaster);
                    final Range insituRange = new Range(minInsitu, maxInsitu);
                    return new ScatterPlot(scatterValues, rasterRange, insituRange);
                } finally {
                    pm.done();
                }
            }

            @Override
            public void done() {
                try {
                    final ScatterPlot scatterPlot = get();
                    final XYIntervalSeries xySeries = scatterPlot.getXySeries();
                    final Range rasterRange = scatterPlot.getRasterRange();
                    final Range insituRange = scatterPlot.getInsituRange();

                    if (xySeries.getItemCount() == 0) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                "Failed to compute scatter plot.\n" +
                                        "No Pixels considered..",
                                /*I18N*/
                                CHART_TITLE, /*I18N*/
                                JOptionPane.ERROR_MESSAGE);
                        plot.setDataset(null);
                        return;
                    }

                    if (xAxisRangeControl.isAutoMinMax()) {
                        xAxisRangeControl.setMin(rasterRange.getMin());
                        xAxisRangeControl.setMax(rasterRange.getMax());
                    } else {
                        rasterRange.setMin(xAxisRangeControl.getMin());
                        rasterRange.setMax(xAxisRangeControl.getMax());
                    }
                    if (yAxisRangeControl.isAutoMinMax()) {
                        yAxisRangeControl.setMin(insituRange.getMin());
                        yAxisRangeControl.setMax(insituRange.getMax());
                    } else {
                        insituRange.setMin(yAxisRangeControl.getMin());
                        insituRange.setMax(yAxisRangeControl.getMax());
                    }

                    setAxisRanges(xAxisRangeControl, plot.getDomainAxis());
                    setAxisRanges(yAxisRangeControl, plot.getRangeAxis());


                    final XYIntervalSeriesCollection dataset = new XYIntervalSeriesCollection();
                    dataset.addSeries(xySeries);
                    plot.setDataset(dataset);

                    final Function2D identityFunction = new Function2D() {
                        @Override
                        public double getValue(double x) {
                            return x;
                        }
                    };
                    final XYSeries identity = DatasetUtilities.sampleFunction2DToSeries(identityFunction, rasterRange.getMin(), rasterRange.getMax(), 100, "identity");
                    final List<XYDataItem> items = identity.getItems();
                    final XYIntervalSeries xyIntervalSeries = new XYIntervalSeries(identity.getKey());
                    for (XYDataItem item : items) {
                        final double confidenceInterval = scatterPlotModel.confidenceInterval;
                        final double x = item.getXValue();
                        final double y = item.getYValue();
                        final double xOff = confidenceInterval * x / 100;
                        final double yOff = confidenceInterval * y / 100;
                        xyIntervalSeries.add(x, x - xOff, x + xOff, y, y - yOff, y + yOff);
                    }
                    final XYIntervalSeriesCollection identityDataset = new XYIntervalSeriesCollection();
                    identityDataset.addSeries(xyIntervalSeries);
                    plot.setDataset(1, identityDataset);


                    plot.getDomainAxis().setLabel(getAxisLabel(raster));

                    final String vdsName = scatterPlotModel.pointDataSource.getName();
                    final String dataFieldName = scatterPlotModel.dataField.getLocalName();
                    plot.getRangeAxis().setLabel(vdsName + " - " + dataFieldName);

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

    private void updateScalingOfXAxis() {
        if (scatterPlotModel.xAxisLogScaled) {
            ValueAxis oldAxis = plot.getDomainAxis();
            if (!(oldAxis instanceof LogarithmicAxis)) {
                LogarithmicAxis logAxisX = new LogarithmicAxis(oldAxis.getLabel());
                logAxisX.setAllowNegativesFlag(true);
                logAxisX.setLog10TickLabelsFlag(true);
                logAxisX.setMinorTickCount(10);
                plot.setDomainAxis(logAxisX);
            }
        } else {
            ValueAxis oldAxis = plot.getDomainAxis();
            if (oldAxis instanceof LogarithmicAxis) {
                final NumberAxis numberAxis = createNumberAxis();
                numberAxis.setLabel(oldAxis.getLabel());
                plot.setDomainAxis(numberAxis);
            }
        }
        setAxisRanges(xAxisRangeControl, plot.getDomainAxis());
    }

    private void updateScalingOfYAxis() {
        if (scatterPlotModel.yAxisLogScaled) {
            ValueAxis oldAxis = plot.getRangeAxis();
            if (!(oldAxis instanceof LogarithmicAxis)) {
                LogarithmicAxis logAxisX = new LogarithmicAxis(oldAxis.getLabel());
                logAxisX.setAllowNegativesFlag(true);
                logAxisX.setLog10TickLabelsFlag(true);
                logAxisX.setAutoRange(yAxisRangeControl.isAutoMinMax());
                logAxisX.setMinorTickCount(10);
                plot.setRangeAxis(logAxisX);
            }
        } else {
            ValueAxis oldAxis = plot.getRangeAxis();
            if (oldAxis instanceof LogarithmicAxis) {
                final NumberAxis numberAxis = createNumberAxis();
                numberAxis.setLabel(oldAxis.getLabel());
                plot.setRangeAxis(numberAxis);
            }
        }
        setAxisRanges(yAxisRangeControl, plot.getRangeAxis());
    }

    private void setAxisRanges(AxisRangeControl axisRangeControl, ValueAxis axis) {
        final boolean autoMinMax = axisRangeControl.isAutoMinMax();
        axis.setAutoRange(autoMinMax);
        if (!autoMinMax) {
            axis.setRange(axisRangeControl.getMin(), axisRangeControl.getMax());
        }
    }

    private String getAxisLabel(RasterDataNode raster) {
        final String unit = raster.getUnit();
        if (unit != null) {
            return raster.getName() + " (" + unit + ")";
        }
        return raster.getName();
    }

    @Override
    protected String getDataAsText() {
//        todo
        return "Must be implemented";
    }
}

