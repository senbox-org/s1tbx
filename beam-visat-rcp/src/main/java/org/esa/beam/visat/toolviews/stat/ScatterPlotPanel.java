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

import com.bc.ceres.binding.*;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.geotools.feature.FeatureCollection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.Range;
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

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static org.esa.beam.visat.toolviews.stat.StatisticChartStyling.getAxisLabel;

/**
 * The scatter plot pane within the statistics window.
 *
 * @author Olaf Danne
 * @author Sabine Embacher
 */
class ScatterPlotPanel extends ChartPagePanel {

    private static final String NO_DATA_MESSAGE = "No scatter plot computed yet.\n" + ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Scatter Plot";

    private final String PROPERTY_NAME_X_AXIS_LOG_SCALED = "xAxisLogScaled";
    private final String PROPERTY_NAME_Y_AXIS_LOG_SCALED = "yAxisLogScaled";
    private final String PROPERTY_NAME_DATA_FIELD = "dataField";
    private final String PROPERTY_NAME_BOX_SIZE = "boxSize";
    private final String PROPERTY_NAME_SHOW_CONFIDENCE_INTERVAL = "showConfidenceInterval";
    private final String PROPERTY_NAME_CONFIDENCE_INTERVAL = "confidenceInterval";


    private final int CONFIDENCE_DSINDEX = 0;
    private final int SCATTERPOINTS_DSINDEX = 1;

    private final ScatterPlotModel scatterPlotModel;
    private final BindingContext bindingContext;
    private final AxisRangeControl xAxisRangeControl;
    private final AxisRangeControl yAxisRangeControl;
    private final XYIntervalSeriesCollection scatterpointsDataset;
    private final XYIntervalSeriesCollection confidenceDataset;

    private final XYPlot plot;

    private ChartPanel scatterPlotDisplay;

    private CorrelativeFieldSelector correlativeFieldSelector;

    private Range xAutoRangeAxisRange;
    private Range yAutoRangeAxisRange;

    ScatterPlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE, true);
        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");
        scatterPlotModel = new ScatterPlotModel();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(scatterPlotModel));
        scatterpointsDataset = new XYIntervalSeriesCollection();
        confidenceDataset = new XYIntervalSeriesCollection();
        plot = new XYPlot();
    }


    @Override
    protected String getDataAsText() {
//        todo
        return "Must be implemented";
    }

    @Override
    protected void initComponents() {
        initParameters();
        createUI();
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();
        if (!isVisible()) {
            return;
        }
        final RasterDataNode raster = getRaster();
        xAxisRangeControl.setTitleSuffix(raster != null ? raster.getName() : null);

        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        yAxisRangeControl.setTitleSuffix(dataField != null ? dataField.getLocalName() : null);

        correlativeFieldSelector.updatePointDataSource(getProduct());
        correlativeFieldSelector.updateDataField();

        // todo - discuss (nf)
        // setChartTitle();

        if (isRasterChanged()) {
            computeCartDataIfPossible();
        }
    }

    @Override
    protected void updateChartData() {
        // todo ... remove ?
        computeCartDataIfPossible();
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
        if (event.getSourceNode() instanceof VectorDataNode) {
            updateComponents();
        }
    }

    private void initParameters() {

        final PropertyChangeListener recomputeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                computeCartDataIfPossible();
            }
        };

        // todo ... insert recompute listener in all properties which triggers a recompute
        bindingContext.addPropertyChangeListener(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK, recomputeListener);
        bindingContext.addPropertyChangeListener(RoiMaskSelector.PROPERTY_NAME_ROI_MASK, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_BOX_SIZE, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_DATA_FIELD, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_SHOW_CONFIDENCE_INTERVAL, recomputeListener);
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_CONFIDENCE_INTERVAL, recomputeListener);

        bindingContext.addPropertyChangeListener(PROPERTY_NAME_X_AXIS_LOG_SCALED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateScalingOfXAxis();
            }
        });
        bindingContext.addPropertyChangeListener(PROPERTY_NAME_Y_AXIS_LOG_SCALED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateScalingOfYAxis();
            }
        });

        xAxisRangeControl.getBindingContext().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateAxis(plot.getDomainAxis(), xAxisRangeControl, xAutoRangeAxisRange);
            }
        });
        yAxisRangeControl.getBindingContext().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateAxis(plot.getRangeAxis(), yAxisRangeControl, yAutoRangeAxisRange);
            }
        });
    }

    private void createUI() {

        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setDataset(CONFIDENCE_DSINDEX, confidenceDataset);
        plot.setDataset(SCATTERPOINTS_DSINDEX, scatterpointsDataset);

        final DeviationRenderer deviationRenderer = new DeviationRenderer(true, false);
        deviationRenderer.setSeriesPaint(0, StatisticChartStyling.SAMPLE_DATA_PAINT);
        deviationRenderer.setSeriesFillPaint(0, StatisticChartStyling.SAMPLE_DATA_FILL_PAINT);
        plot.setRenderer(CONFIDENCE_DSINDEX, deviationRenderer);

        final XYErrorRenderer xyErrorRenderer = new XYErrorRenderer();
        xyErrorRenderer.setDrawXError(true);
        xyErrorRenderer.setErrorStroke(new BasicStroke(1));
        xyErrorRenderer.setErrorPaint(StatisticChartStyling.CORRELATIVE_POINT_FILL_PAINT);
        xyErrorRenderer.setSeriesShape(0, StatisticChartStyling.CORRELATIVE_POINT_SHAPE);
        xyErrorRenderer.setSeriesOutlinePaint(0, StatisticChartStyling.CORRELATIVE_POINT_OUTLINE_PAINT);
        xyErrorRenderer.setSeriesFillPaint(0, StatisticChartStyling.CORRELATIVE_POINT_FILL_PAINT);
        xyErrorRenderer.setSeriesShapesFilled(0, StatisticChartStyling.CORRELATIVE_POINT_SHAPES_FILLED);
        xyErrorRenderer.setSeriesLinesVisible(0, false);
        xyErrorRenderer.setSeriesShapesVisible(0, true);
        xyErrorRenderer.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
        xyErrorRenderer.setSeriesToolTipGenerator(0, new XYPlotToolTipGenerator());
        plot.setRenderer(SCATTERPOINTS_DSINDEX, xyErrorRenderer);

        final boolean autoRangeIncludesZero = false;
        plot.setDomainAxis(StatisticChartStyling.createNumberAxis(null, autoRangeIncludesZero));
        plot.setRangeAxis(StatisticChartStyling.createNumberAxis(null, autoRangeIncludesZero));

        JFreeChart chart = new JFreeChart(CHART_TITLE, plot);
        ChartFactory.getChartTheme().apply(chart);
        chart.removeLegend();

        createUI(createChartPanel(chart), createInputParameterPanel(), bindingContext);
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
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getMinX(), bounds.getMaxX());
            }

            protected String createMaskExpression(double x1, double x2) {
                String bandName = BandArithmetic.createExternalName(getRaster().getName());
                return String.format("%s >= %s && %s <= %s", bandName, x1, bandName, x2);
            }
        };
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        scatterPlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        scatterPlotDisplay.getPopupMenu().addSeparator();
        scatterPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return scatterPlotDisplay;
    }

    private JPanel createInputParameterPanel() {
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
        bindingContext.bind(PROPERTY_NAME_X_AXIS_LOG_SCALED, xLogCheck);
        final JPanel xAxisOptionPanel = new JPanel(new BorderLayout());
        xAxisOptionPanel.add(xAxisRangeControl.getPanel());
        xAxisOptionPanel.add(xLogCheck, BorderLayout.SOUTH);

        final JCheckBox yLogCheck = new JCheckBox("Log scaled");
        bindingContext.bind(PROPERTY_NAME_Y_AXIS_LOG_SCALED, yLogCheck);
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

    private void updateAxis(ValueAxis axis, AxisRangeControl axisRangeControl, Range range) {
        setAxisRanges(axisRangeControl, axis);
        setAutorangeAxisValues(axisRangeControl, axis, range);
    }

    private void setAxisRanges(AxisRangeControl axisRangeControl, ValueAxis axis) {
        final boolean autoMinMax = axisRangeControl.isAutoMinMax();
        axis.setAutoRange(autoMinMax);
        if (!autoMinMax) {
            setRangeFromRangeControl(axisRangeControl, axis);
        }
    }

    private void setAutorangeAxisValues(AxisRangeControl axisRangeControl, ValueAxis axis, Range range) {
        if (axisRangeControl.isAutoMinMax()) {
            axis.setRange(range);
            setAxisRangeControlMinMax(axisRangeControl, axis);
        }
    }

    private void setAxisRangeControlMinMax(AxisRangeControl axisRangeControl, ValueAxis axis) {
        if (axisRangeControl.isAutoMinMax()) {
            axisRangeControl.setMin(cropToDecimals(axis.getLowerBound(), 3));
            axisRangeControl.setMax(cropToDecimals(axis.getUpperBound(), 3));
        }
    }

    private void setRangeFromRangeControl(AxisRangeControl axisRangeControl, ValueAxis axis) {
        axis.setRange(axisRangeControl.getMin(), axisRangeControl.getMax());
    }

    /*
    private void setChartTitle() {
        final String xAxisName;
        if (getRaster() != null) {
            xAxisName = getAxisLabel(getRaster());
        } else {
            xAxisName = "";
        }

        final String yAxisName;
        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (dataField != null) {
            yAxisName = dataField.getLocalName();
        } else {
            yAxisName = "";
        }

        final String subtitle;
        if (xAxisName.length() == 0 && xAxisName.length() == 0) {
            subtitle = "";
        } else {
            subtitle = MessageFormat.format("{0}, {1}", xAxisName, yAxisName);
        }

        final JFreeChart chart = scatterPlotDisplay.getChart();
        final List<Title> subtitles = new ArrayList<Title>();
        subtitles.add(new TextTitle(subtitle));
        chart.setSubtitles(subtitles);
    }
    */

    private void updateScalingOfXAxis() {
        final boolean logScaled = scatterPlotModel.xAxisLogScaled;
        final ValueAxis oldAxis = plot.getDomainAxis();
        ValueAxis newAxis = StatisticChartStyling.updateScalingOfAxis(logScaled, oldAxis, false);
        newAxis.setRange(oldAxis.getRange());
        newAxis.setAutoRange(false);
        plot.setDomainAxis(newAxis);
    }

    private void updateScalingOfYAxis() {
        final boolean logScaled = scatterPlotModel.yAxisLogScaled;
        final ValueAxis oldAxis = plot.getRangeAxis();
        ValueAxis newAxis = StatisticChartStyling.updateScalingOfAxis(logScaled, oldAxis, false);
        newAxis.setRange(oldAxis.getRange());
        newAxis.setAutoRange(false);
        plot.setRangeAxis(newAxis);
    }

    private void computeCartDataIfPossible() {
        if (scatterPlotModel.pointDataSource != null && scatterPlotModel.dataField != null && getRaster() != null) {
            compute(scatterPlotModel.useRoiMask ? scatterPlotModel.roiMask : null);
        } else {
            scatterpointsDataset.removeAllSeries();
            confidenceDataset.removeAllSeries();
        }
    }

    private void compute(final Mask selectedMask) {

        final RasterDataNode raster = getRaster();

        final AttributeDescriptor dataField = scatterPlotModel.dataField;
        if (raster == null || dataField == null) {
            return;
        }

        ProgressMonitorSwingWorker<XYIntervalSeries, Object> swingWorker = new ProgressMonitorSwingWorker<XYIntervalSeries, Object>(
                this, "Computing scatter plot") {

            @Override
            protected XYIntervalSeries doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing scatter plot...", 100);
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

                        final int centerIndex = box.width * (box.height / 2) + (box.width / 2);
                        if (maskBuffer[centerIndex] == 0) {
                            continue;
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
                        double rasterSigma = n > 1 ? Math.sqrt((sumSqr - (sum * sum) / n) / (n - 1)) : 0.0;

                        String localName = dataField.getLocalName();
                        Number attribute = (Number) feature.getAttribute(localName);
                        scatterValues.add(rasterMean, rasterMean - rasterSigma, rasterMean + rasterSigma,
                                          attribute.doubleValue(), attribute.doubleValue(), attribute.doubleValue());
                    }
                } finally {
                    pm.done();
                }
                return scatterValues;
            }

            @Override
            public void done() {
                try {
                    scatterpointsDataset.removeAllSeries();
                    confidenceDataset.removeAllSeries();

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

                    final ValueAxis xAxis = plot.getDomainAxis();
                    final ValueAxis yAxis = plot.getRangeAxis();

                    scatterpointsDataset.addSeries(xySeries);

                    xAxis.setAutoRange(true);
                    yAxis.setAutoRange(true);
                    xAxis.configure();
                    yAxis.configure();

                    xAutoRangeAxisRange = new Range(xAxis.getLowerBound(), xAxis.getUpperBound());
                    yAutoRangeAxisRange = new Range(yAxis.getLowerBound(), yAxis.getUpperBound());

                    setAxisRanges(xAxisRangeControl, xAxis);
                    setAxisRanges(yAxisRangeControl, yAxis);

                    setAxisRangeControlMinMax(xAxisRangeControl, xAxis);
                    setAxisRangeControlMinMax(yAxisRangeControl, yAxis);

                    xAxis.setAutoRange(false);
                    yAxis.setAutoRange(false);

                    confidenceDataset.addSeries(computeConfidenceData(xAxis.getLowerBound(), xAxis.getUpperBound()));

                    xAxis.setLabel(getAxisLabel(raster, "X", false));

                    final String vdsName = scatterPlotModel.pointDataSource.getName();
                    final String dataFieldName = scatterPlotModel.dataField.getLocalName();
                    yAxis.setLabel(vdsName + " - " + dataFieldName);

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

    private XYIntervalSeries computeConfidenceData(double lowerBound, double upperBound) {
        final Function2D identityFunction = new Function2D() {
            @Override
            public double getValue(double x) {
                return x;
            }
        };

        final XYSeries identity = DatasetUtilities.sampleFunction2DToSeries(identityFunction, lowerBound, upperBound, 100, "identity");
        final XYIntervalSeries xyIntervalSeries = new XYIntervalSeries(identity.getKey());
        final List<XYDataItem> items = identity.getItems();
        for (XYDataItem item : items) {
            final double x = item.getXValue();
            final double y = item.getYValue();
            if (scatterPlotModel.showConfidenceInterval) {
                final double confidenceInterval = scatterPlotModel.confidenceInterval;
                final double xOff = confidenceInterval * x / 100;
                final double yOff = confidenceInterval * y / 100;
                xyIntervalSeries.add(x, x - xOff, x + xOff, y, y - yOff, y + yOff);
            } else {
                xyIntervalSeries.add(x, x, x, y, y, y);
            }
        }
        return xyIntervalSeries;
    }

    private double cropToDecimals(double value, final int numDecimals) {
        final double pow = Math.pow(10, numDecimals);
        final double reverse = 1 / pow;
        return Math.round(value * pow) * reverse;
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

