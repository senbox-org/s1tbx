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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.geotools.feature.FeatureCollection;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * The scatter plot pane within the statistics window.
 *
 * @author Olaf Danne
 * @author Sabine Embacher
 */
class ScatterPlotPanel extends ChartPagePanel {

    private static final String NO_DATA_MESSAGE = "No scatter plot computed yet.\n" + ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Scatter Plot";

    private ChartPanel scatterPlotDisplay;
    private XYPlot plot;

    private ScatterPlotModel scatterPlotModel;
    private BindingContext bindingContext;

    private final String PARAM_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PARAM_Y_AXIS_LOG_SCALED = "yAxisLogScaled";

    private CorrelativeFieldSelector correlativeFieldSelector;
    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;

    private boolean isInitialized;

    ScatterPlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE, true);
    }

    public void compute(final Mask selectedMask) {

        final RasterDataNode raster = getRaster();

        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (raster == null || dataField == null) {
            return;
        }

        ProgressMonitorSwingWorker<XYIntervalSeries, Object> swingWorker = new ProgressMonitorSwingWorker<XYIntervalSeries, Object>(
                this, "Computing scatter plot") {

            @Override
            protected XYIntervalSeries doInBackground(ProgressMonitor pm) throws Exception {
//                pm.beginTask("Computing scatter plot...", 100);
                final XYIntervalSeries scatterValues = new XYIntervalSeries("scatter values");
                try {
                    final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = scatterPlotModel.pointDataSource.getFeatureCollection();
                    final SimpleFeature[] features = collection.toArray(new SimpleFeature[collection.size()]);

                    final int boxSize = scatterPlotModel.boxSize;

                    final Rectangle sceneRect = new Rectangle(raster.getSceneRasterWidth(), raster.getSceneRasterHeight());

                    for (SimpleFeature feature : features) {
                        final com.vividsolutions.jts.geom.Point point;
                        point = (com.vividsolutions.jts.geom.Point) feature.getDefaultGeometryProperty().getValue();
                        final int centerX = (int) point.getX();
                        final int centerY = (int) point.getY();

                        if (!sceneRect.contains(centerX, centerY)) {
                            continue;
                        }
                        final Rectangle box = sceneRect.intersection(new Rectangle(centerX - boxSize / 2,
                                centerY - boxSize / 2,
                                boxSize, boxSize));
                        if (box.isEmpty()) {
                            continue;
                        }
                        final double[] rasterValues = new double[box.width * box.height];
                        raster.readPixels(box.x, box.y, box.width, box.height, rasterValues);

                        final int[] maskBuffer = new int[box.width * box.height];
                        Arrays.fill(maskBuffer, 1);
                        if (selectedMask != null) {
                            selectedMask.readPixels(box.x, box.y, box.width, box.height, maskBuffer);
                        }

                        double sum = 0;
                        double sumSqr = 0;
                        int n = 0;

                        for (int y = 0; y < box.height; y++) {
                            for (int x = 0; x < box.width; x++) {
                                final int index = y * box.height + x;
                                if (raster.isPixelValid(x + box.x, y + box.y) && maskBuffer[index] != 0) {
                                    final double rasterValue = rasterValues[index];
                                    sum += rasterValue;
                                    sumSqr += rasterValue * rasterValue;
                                    n++;
                                }
                            }
                        }

                        double rasterMean = sum / n;
                        double rasterSigma = n > 1 ? (float) Math.sqrt(1.0 / (n - 1.0) * (sumSqr - (sum * sum) / n)) : 0.0F;
                        final double x = rasterMean;
                        final double dx = rasterSigma;

                        String localName = dataField.getLocalName();
                        Number attribute = (Number) feature.getAttribute(localName);
                        final double insituValue = attribute.doubleValue();
                        final double y = insituValue;
                        scatterValues.add(x, x - dx, x + dx, y, y, y);
                    }
                } finally {
//                    pm.done();
                    return scatterValues;
                }
            }

            @Override
            public void done() {
                try {
                    plot.setDataset(0, null);
                    plot.setDataset(1, null);
                    final XYIntervalSeries xySeries = get();

                    if (xySeries.getItemCount() == 0) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                "Failed to compute scatter plot.\n" +
                                        "No Pixels considered..",
                                /*I18N*/
                                CHART_TITLE, /*I18N*/
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    final ValueAxis rasterAxis = plot.getDomainAxis();
                    final ValueAxis insituAxis = plot.getRangeAxis();

                    setAxisRanges(xAxisRangeControl, rasterAxis);
                    setAxisRanges(yAxisRangeControl, insituAxis);

                    final XYIntervalSeriesCollection dataset = new XYIntervalSeriesCollection();
                    dataset.addSeries(xySeries);
                    plot.setDataset(0, dataset);

                    if (xAxisRangeControl.isAutoMinMax()) {
                        xAxisRangeControl.setMin(cropToDecimals(rasterAxis.getLowerBound(), 3));
                        xAxisRangeControl.setMax(cropToDecimals(rasterAxis.getUpperBound(), 3));
                    }
                    if (yAxisRangeControl.isAutoMinMax()) {
                        yAxisRangeControl.setMin(cropToDecimals(insituAxis.getLowerBound(), 3));
                        yAxisRangeControl.setMax(cropToDecimals(insituAxis.getUpperBound(), 3));
                    }

                    rasterAxis.setAutoRange(false);
                    insituAxis.setAutoRange(false);

                    final Function2D identityFunction = new Function2D() {
                        @Override
                        public double getValue(double x) {
                            return x;
                        }
                    };

                    if (scatterPlotModel.showConfidenceInterval) {
                        final XYSeries identity = DatasetUtilities.sampleFunction2DToSeries(identityFunction, rasterAxis.getLowerBound(), rasterAxis.getUpperBound(), 100, "identity");
                        final XYIntervalSeries xyIntervalSeries = new XYIntervalSeries(identity.getKey());
                        final List<XYDataItem> items = identity.getItems();
                        for (XYDataItem item : items) {
                            final double x = item.getXValue();
                            final double y = item.getYValue();
                            final double confidenceInterval = scatterPlotModel.confidenceInterval;
                            final double xOff = confidenceInterval * x / 100;
                            final double yOff = confidenceInterval * y / 100;
                            xyIntervalSeries.add(x, x - xOff, x + xOff, y, y - yOff, y + yOff);
                        }
                        final XYIntervalSeriesCollection identityDataset = new XYIntervalSeriesCollection();
                        identityDataset.addSeries(xyIntervalSeries);
                        plot.setDataset(1, identityDataset);
                    } else {
                        plot.setDataset(1, DatasetUtilities.sampleFunction2D(identityFunction, rasterAxis.getLowerBound(), rasterAxis.getUpperBound(), 100, "identity"));
                    }

                    rasterAxis.setLabel(getAxisLabel(raster));

                    final String vdsName = scatterPlotModel.pointDataSource.getName();
                    final String dataFieldName = scatterPlotModel.dataField.getLocalName();
                    insituAxis.setLabel(vdsName + " - " + dataFieldName);

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

    private double cropToDecimals(double value, final int numDecimals) {
        final double pow = Math.pow(10, numDecimals);
        final double reverse = 1 / pow;
        return Math.round(value * pow) * reverse;
    }

    @Override
    protected String getDataAsText() {
//        todo
        return "Must be implemented";
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
        isInitialized = true;
        updateContent();
    }

    @Override
    protected void setRaster(RasterDataNode raster) {
        super.setRaster(raster);
        if (isInitialized) {
            xAxisRangeControl.setTitleSuffix(raster != null ? raster.getName() : null);
            updateUIState();
        }
    }

    @Override
    protected void updateContent() {
        if (!isInitialized || !isVisible()) {
            return;
        }
        plot.setDataset(0, null);
        plot.setDataset(1, null);
        compute();

        super.updateContent();
        correlativeFieldSelector.updatePointDataSource(getProduct());
        correlativeFieldSelector.updateDataField();

        setChartTitle();
    }

    @Override
    protected void compute() {
        if (scatterPlotModel.pointDataSource != null && scatterPlotModel.dataField != null && getRaster() != null) {
            compute(scatterPlotModel.useRoiMask ? scatterPlotModel.roiMask : null);
        }
    }

    private NumberAxis createNumberAxis() {
        final NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        return rangeAxis;
    }

    private ChartPanel createChartPanel(JFreeChart chart) {
        scatterPlotDisplay = new ChartPanel(chart) {
            @Override
            public void restoreAutoBounds() {
                // here we tweak the notify flag on the plot so that only
                // one notification happens even though we update multiple
                // axes...
                boolean savedNotify = plot.isNotify();
                plot.setNotify(false);
                setRangeFromRangeControl(xAxisRangeControl, plot.getDomainAxis());
                setRangeFromRangeControl(yAxisRangeControl, plot.getRangeAxis());
                plot.setNotify(savedNotify);
            }
        };

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                scatterPlotDisplay,
                "scatter_plot_area",
                "Mask generated from selected scatter plot area",
                Color.RED,
                PlotAreaSelectionTool.AreaType.X_RANGE) {
            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, double x0, double y0, double dx, double dy) {
                return String.format("%s >= %s && %s <= %s",
                        getRaster().getName(),
                        x0,
                        getRaster().getName(),
                        x0 + dx);
            }
        };
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return scatterPlotDisplay;
    }

    private JPanel createMiddlePanel() {
        final PropertyDescriptor boxSizeDescriptor = bindingContext.getPropertySet().getDescriptor("boxSize");
        boxSizeDescriptor.setValueRange(new ValueRange(1, 101));
        boxSizeDescriptor.setAttribute("stepSize", 2);
        boxSizeDescriptor.setValidator(new Validator() {
            @Override
            public void validateValue(Property property, Object value) throws ValidationException {
                if (((Number) value).intValue() % 2 == 0) {
                    throw new ValidationException("Only odd values allowed as box size.");
                }
            }
        });
        final JSpinner boxSizeSpinner = new JSpinner();
        bindingContext.bind("boxSize", boxSizeSpinner);

        final JPanel boxSizePanel = new JPanel(new BorderLayout(5, 3));
        boxSizePanel.add(new JLabel("Box size:"), BorderLayout.WEST);
        boxSizePanel.add(boxSizeSpinner);

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
        final JLabel percentLabel = new JLabel(" %");
        bindingContext.bind("showConfidenceInterval", confidenceCheck);
        bindingContext.bind("confidenceInterval", confidenceField);
        bindingContext.getBinding("confidenceInterval").addComponent(percentLabel);
        bindingContext.bindEnabledState("confidenceInterval", true, "showConfidenceInterval", true);
        final JPanel confidencePanel = new JPanel(new BorderLayout(5, 3));
        confidencePanel.add(confidenceCheck, BorderLayout.NORTH);
        confidencePanel.add(confidenceField);
        confidencePanel.add(percentLabel, BorderLayout.EAST);

        // UI arrangement

        JPanel middlePanel = GridBagUtils.createPanel();
        GridBagConstraints middlePanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=6,weighty=0,weightx=1");
        GridBagUtils.addToPanel(middlePanel, boxSizePanel, middlePanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(middlePanel, pointDataSourcePanel, middlePanelConstraints, "gridy=1");
        GridBagUtils.addToPanel(middlePanel, pointDataFieldPanel, middlePanelConstraints, "gridy=2");
        GridBagUtils.addToPanel(middlePanel, xAxisOptionPanel, middlePanelConstraints, "gridy=3");
        GridBagUtils.addToPanel(middlePanel, yAxisOptionPanel, middlePanelConstraints, "gridy=4");
        GridBagUtils.addToPanel(middlePanel, new JSeparator(), middlePanelConstraints, "gridy=5");
        GridBagUtils.addToPanel(middlePanel, confidencePanel, middlePanelConstraints, "gridy=6,fill=HORIZONTAL");

        return middlePanel;
    }

    private void createUI() {
        // Plot
        plot = new XYPlot();
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        final XYErrorRenderer xyErrorRenderer = new XYErrorRenderer();
        xyErrorRenderer.setDrawXError(true);
        xyErrorRenderer.setErrorStroke(new BasicStroke(1));
        xyErrorRenderer.setErrorPaint(new Color(255, 150, 150));

        xyErrorRenderer.setSeriesShape(0, new Ellipse2D.Float(-4, -4, 8, 8));
        xyErrorRenderer.setSeriesLinesVisible(0, false);
        xyErrorRenderer.setSeriesShapesVisible(0, true);
        xyErrorRenderer.setSeriesPaint(0, null);
        xyErrorRenderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        xyErrorRenderer.setSeriesOutlinePaint(0, new Color(200, 0, 0));
        xyErrorRenderer.setSeriesFillPaint(0, new Color(255, 150, 150));
        xyErrorRenderer.setSeriesShapesFilled(0, true);
        xyErrorRenderer.setSeriesToolTipGenerator(0, new XYPlotToolTipGenerator());
        plot.setRenderer(0, xyErrorRenderer);

        final DeviationRenderer deviationRenderer = new DeviationRenderer(true, false);
        deviationRenderer.setSeriesPaint(0, Color.black);
        deviationRenderer.setSeriesFillPaint(0, Color.lightGray);
        plot.setRenderer(1, deviationRenderer);
        plot.setDomainAxis(createNumberAxis());
        plot.setRangeAxis(createNumberAxis());

        JFreeChart chart = new JFreeChart(CHART_TITLE, plot);
        chart.removeLegend();

        createUI(createChartPanel(chart), createMiddlePanel(), bindingContext);

        updateUIState();
    }

    private String getAxisLabel(RasterDataNode raster) {
        final String unit = raster.getUnit();
        if (unit != null) {
            return raster.getName() + " (" + unit + ")";
        }
        return raster.getName();
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

    private void setAxisRanges(AxisRangeControl axisRangeControl, ValueAxis axis) {
        final boolean autoMinMax = axisRangeControl.isAutoMinMax();
        axis.setAutoRange(autoMinMax);
        if (!autoMinMax) {
            setRangeFromRangeControl(axisRangeControl, axis);
        }
    }

    private void setRangeFromRangeControl(AxisRangeControl axisRangeControl, ValueAxis axis) {
        axis.setRange(axisRangeControl.getMin(), axisRangeControl.getMax());
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

    private void updateScalingOfXAxis() {
        if (scatterPlotModel.xAxisLogScaled) {
            ValueAxis oldAxis = plot.getDomainAxis();
            if (!(oldAxis instanceof CustomLogarithmicAxis)) {
                CustomLogarithmicAxis logAxisX = new CustomLogarithmicAxis(oldAxis.getLabel());
//                LogarithmicAxis logAxisX = new MyLogarithmicAxis(oldAxis.getLabel());
                logAxisX.setAllowNegativesFlag(true);
                logAxisX.setLog10TickLabelsFlag(true);
                logAxisX.setMinorTickCount(10);
                plot.setDomainAxis(logAxisX);
            }
        } else {
            ValueAxis oldAxis = plot.getDomainAxis();
            if (oldAxis instanceof CustomLogarithmicAxis) {
                final NumberAxis numberAxis = createNumberAxis();
                numberAxis.setLabel(oldAxis.getLabel());
                plot.setDomainAxis(numberAxis);
            }
            setAxisRanges(xAxisRangeControl, plot.getDomainAxis());
        }
//        setAxisRanges(xAxisRangeControl, plot.getDomainAxis());
    }

    private void updateScalingOfYAxis() {
        if (scatterPlotModel.yAxisLogScaled) {
            ValueAxis oldAxis = plot.getRangeAxis();
            if (!(oldAxis instanceof CustomLogarithmicAxis)) {
                CustomLogarithmicAxis logAxisX = new CustomLogarithmicAxis(oldAxis.getLabel());
                logAxisX.setAllowNegativesFlag(true);
                logAxisX.setLog10TickLabelsFlag(true);
                logAxisX.setAutoRange(yAxisRangeControl.isAutoMinMax());
                logAxisX.setMinorTickCount(10);
                plot.setRangeAxis(logAxisX);
            }
        } else {
            ValueAxis oldAxis = plot.getRangeAxis();
            if (oldAxis instanceof CustomLogarithmicAxis) {
                final NumberAxis numberAxis = createNumberAxis();
                numberAxis.setLabel(oldAxis.getLabel());
                plot.setRangeAxis(numberAxis);
            }
        }
        setAxisRanges(yAxisRangeControl, plot.getRangeAxis());
    }

    private void updateUIState() {
        if (!isInitialized || !isVisible()) {
            return;
        }
        setChartTitle();
        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        yAxisRangeControl.setTitleSuffix(dataField != null ? dataField.getLocalName() : null);
        updateContent();
    }

    private static class ScatterPlotModel {

        private int boxSize = 1; // Don´t remove this field, it is be used via binding
        private boolean useRoiMask; // Don´t remove this field, it is be used via binding
        private Mask roiMask; // Don´t remove this field, it is be used via binding
        private VectorDataNode pointDataSource; // Don´t remove this field, it is be used via binding
        private AttributeDescriptor dataField; // Don´t remove this field, it is be used via binding
        private boolean xAxisLogScaled; // Don´t remove this field, it is be used via binding
        private boolean yAxisLogScaled; // Don´t remove this field, it is be used via binding
        private boolean showConfidenceInterval; // Don´t remove this field, it is be used via binding
        private double confidenceInterval = 15; // Don´t remove this field, it is be used via binding
    }
}

