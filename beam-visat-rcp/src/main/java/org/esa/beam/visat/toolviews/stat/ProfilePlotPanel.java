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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.nav.CursorSynchronizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.esa.beam.visat.toolviews.stat.StatisticChartStyling.getCorrelativeDataLabel;

/**
 * The profile plot pane within the statistics window.
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 */
class ProfilePlotPanel extends ChartPagePanel {

    public static final String CHART_TITLE = "Profile Plot";
    private static final String NO_DATA_MESSAGE = "No profile plot computed yet.\n" +
            "It will be computed if vector data (a polygon, an ellipse, or a line)\n"+
            "is selected within the image view.\n" +
            HELP_TIP_MESSAGE+"\n"+
            ZOOM_TIP_MESSAGE;

    public static final String PROPERTY_NAME_MARK_SEGMENTS = "markSegments";
    public static final String PROPERTY_NAME_LOG_SCALED = "logScaled";
    public static final String DEFAULT_SAMPLE_DATASET_NAME = "Sample";

    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;

    private boolean isInitialized;

    private ChartPanel profilePlotDisplay;
    private JFreeChart chart;
    private XYIntervalSeriesCollection dataset;
    private TransectProfileData profileData;

    private boolean axisAdjusting = false;

    private Set<IntervalMarker> intervalMarkers;
    private CorrelativeFieldSelector correlativeFieldSelector;
    private DataSourceConfig dataSourceConfig;
    private DeviationRenderer deviationRenderer;
    private XYErrorRenderer pointRenderer;
    private Enablement pointDataSourceEnablement;
    private Enablement dataFieldEnablement;
    private CursorSynchronizer cursorSynchronizer;

    ProfilePlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId, CHART_TITLE, false);
    }

    private ChartPanel createChartPanel(JFreeChart chart) {
        profilePlotDisplay = new ChartPanel(chart);

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                                                                                         profilePlotDisplay,
                                                                                         "profile_plot_area",
                                                                                         "Mask generated from selected profile plot area",
                                                                                         Color.RED,
                                                                                         PlotAreaSelectionTool.AreaType.Y_RANGE) {

            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getMinY(), bounds.getMaxY());
            }

            protected String createMaskExpression(double x1, double x2) {
                String bandName = BandArithmetic.createExternalName(getRaster().getName());
                return String.format("%s >= %s && %s <= %s", bandName, x1, bandName, x2);
            }
        };


        profilePlotDisplay.addChartMouseListener(new XYPlotMarker(profilePlotDisplay, new XYPlotMarker.Listener() {
            @Override
            public void pointSelected(XYDataset xyDataset, int seriesIndex, Point2D dataPoint) {
                if (profileData != null) {
                    GeoPos[] geoPositions = profileData.getGeoPositions();
                    int index = (int) dataPoint.getX();
                    if (index >= 0 && index < geoPositions.length) {
                        if (cursorSynchronizer == null) {
                            cursorSynchronizer = new CursorSynchronizer(VisatApp.getApp());
                        }
                        if (!cursorSynchronizer.isEnabled()) {
                            cursorSynchronizer.setEnabled(true);
                        }
                        cursorSynchronizer.updateCursorOverlays(geoPositions[index]);
                    }
                }
            }

            @Override
            public void pointDeselected() {
                cursorSynchronizer.setEnabled(false);
            }
        }));
        profilePlotDisplay.setInitialDelay(200);
        profilePlotDisplay.setDismissDelay(1500);
        profilePlotDisplay.setReshowDelay(200);
        profilePlotDisplay.setZoomTriggerDistance(5);
        profilePlotDisplay.getPopupMenu().addSeparator();
        profilePlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        profilePlotDisplay.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        profilePlotDisplay.getPopupMenu().addSeparator();
        profilePlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());

        return profilePlotDisplay;
    }

    @Override
    protected void showAlternativeView() {
        final TableModel model;
        if (profileData != null) {
            model = createProfileDataTableModel();
        } else {
            model = new DefaultTableModel();
        }
        final TableViewPagePanel alternativPanel = (TableViewPagePanel)getAlternativeView();
        alternativPanel.setModel(model);
        super.showAlternativeView();
    }

    @Override
    protected void initComponents() {
        getAlternativeView().initComponents();
        dataset = new XYIntervalSeriesCollection();
        this.chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                "Path in pixels",
                DEFAULT_SAMPLE_DATASET_NAME,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        final XYPlot plot = chart.getXYPlot();

        deviationRenderer = new DeviationRenderer();
        deviationRenderer.setUseFillPaint(true);
        deviationRenderer.setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        deviationRenderer.setSeriesLinesVisible(0, true);
        deviationRenderer.setSeriesShapesVisible(0, false);
        deviationRenderer.setSeriesStroke(0, new BasicStroke(1.0f));
        deviationRenderer.setSeriesPaint(0, StatisticChartStyling.SAMPLE_DATA_PAINT);
        deviationRenderer.setSeriesFillPaint(0, StatisticChartStyling.SAMPLE_DATA_FILL_PAINT);

        pointRenderer = new XYErrorRenderer();
        pointRenderer.setUseFillPaint(true);
        pointRenderer.setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        pointRenderer.setSeriesLinesVisible(0, false);
        pointRenderer.setSeriesShapesVisible(0, true);
        pointRenderer.setSeriesStroke(0, new BasicStroke(1.0f));
        pointRenderer.setSeriesPaint(0, StatisticChartStyling.SAMPLE_DATA_PAINT);
        pointRenderer.setSeriesFillPaint(0, StatisticChartStyling.SAMPLE_DATA_FILL_PAINT);
        pointRenderer.setSeriesShape(0, StatisticChartStyling.SAMPLE_DATA_POINT_SHAPE);

        configureRendererForCorrelativeData(deviationRenderer);
        configureRendererForCorrelativeData(pointRenderer);

        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setRenderer(deviationRenderer);

        final AxisChangeListener axisListener = new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent event) {
                adjustAxisControlComponents();
            }
        };

        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();
        // allow transfer from bounds into min/max fields, if auto min/maxis enabled
        domainAxis.setAutoRange(true);
        rangeAxis.setAutoRange(true);

        domainAxis.addChangeListener(axisListener);
        rangeAxis.addChangeListener(axisListener);

        intervalMarkers = new HashSet<IntervalMarker>();

        xAxisRangeControl = new AxisRangeControl("X-Axis");
        yAxisRangeControl = new AxisRangeControl("Y-Axis");

        final PropertyChangeListener changeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(PROPERTY_NAME_MARK_SEGMENTS)) {
                    updateDataSet();
                }
                if (evt.getPropertyName().equals(PROPERTY_NAME_LOG_SCALED)) {
                    updateScalingOfYAxis();
                }
                updateUIState();
            }
        };
        xAxisRangeControl.getBindingContext().addPropertyChangeListener(changeListener);
        xAxisRangeControl.getBindingContext().getPropertySet().addProperty(Property.create(PROPERTY_NAME_MARK_SEGMENTS, false));
        xAxisRangeControl.getBindingContext().getPropertySet().getDescriptor(PROPERTY_NAME_MARK_SEGMENTS).setDescription("Toggle whether to mark segments");

        yAxisRangeControl.getBindingContext().addPropertyChangeListener(changeListener);
        yAxisRangeControl.getBindingContext().getPropertySet().addProperty(Property.create(PROPERTY_NAME_LOG_SCALED, false));
        yAxisRangeControl.getBindingContext().getPropertySet().getDescriptor(PROPERTY_NAME_LOG_SCALED).setDescription("Toggle whether to use a logarithmic axis");

        dataSourceConfig = new DataSourceConfig();
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        JPanel middlePanel = createMiddlePanel(bindingContext);
        createUI(createChartPanel(chart), middlePanel, bindingContext);

        isInitialized = true;

        updateComponents();
    }

    protected JPanel createMiddlePanel(BindingContext bindingContext) {
        final JLabel boxSizeLabel = new JLabel("Box size: ");
        final JSpinner boxSizeSpinner = new JSpinner();
        final JCheckBox computeInBetweenPoints = new JCheckBox("Compute in-between points");
        final JCheckBox useCorrelativeData = new JCheckBox("Use correlative data");

        correlativeFieldSelector = new CorrelativeFieldSelector(bindingContext);
        final PropertyDescriptor boxSizeDescriptor = bindingContext.getPropertySet().getProperty("boxSize").getDescriptor();
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
        bindingContext.bind("boxSize", boxSizeSpinner);
        bindingContext.bind("computeInBetweenPoints", computeInBetweenPoints);
        bindingContext.bind("useCorrelativeData", useCorrelativeData);
        EnablePointDataCondition condition = new EnablePointDataCondition();
        pointDataSourceEnablement = bindingContext.bindEnabledState("pointDataSource", true, condition);
        dataFieldEnablement = bindingContext.bindEnabledState("dataField", true, condition);

        bindingContext.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateDataSource();
                updateDataSet();
                updateUIState();
            }
        });

        JPanel dataSourceOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints dataSourceOptionsConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, boxSizeLabel, dataSourceOptionsConstraints, "gridwidth=1,gridy=0,gridx=0,weightx=0,insets.left=4");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, boxSizeSpinner, dataSourceOptionsConstraints, "gridwidth=1,gridy=0,gridx=1,weightx=1,insets.left=0");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, computeInBetweenPoints, dataSourceOptionsConstraints, "gridwidth=2,gridy=1,gridx=0,weightx=2");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, useCorrelativeData, dataSourceOptionsConstraints, "gridy=2,insets.top=16");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.pointDataSourceLabel, dataSourceOptionsConstraints, "gridy=3,insets.top=0,insets.left=4");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.pointDataSourceList, dataSourceOptionsConstraints, "gridy=4,insets.left=4");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.dataFieldLabel, dataSourceOptionsConstraints, "gridy=5,insets.left=4");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.dataFieldList, dataSourceOptionsConstraints, "gridy=6,insets.left=4");

        xAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_MARK_SEGMENTS, new JCheckBox("Mark segments"));
        yAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_LOG_SCALED, new JCheckBox("Log10 scaled"));

        JPanel displayOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints displayOptionsConstraints = GridBagUtils.createConstraints("anchor=SOUTH,fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=0");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_MARK_SEGMENTS).getComponents()[0], displayOptionsConstraints, "gridy=1");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=2");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getComponents()[0], displayOptionsConstraints, "gridy=3");

        JPanel middlePanel = GridBagUtils.createPanel();
        GridBagConstraints middlePanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(middlePanel, dataSourceOptionsPanel, middlePanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(middlePanel, new JPanel(), middlePanelConstraints, "gridy=1,fill=VERTICAL,weighty=1");
        GridBagUtils.addToPanel(middlePanel, displayOptionsPanel, middlePanelConstraints, "gridy=2,fill=HORIZONTAL,weighty=0");

        return middlePanel;
    }

    @Override
    protected void updateChartData() {
        //Left empty for Profile Plot Panel
    }

    private void configureRendererForCorrelativeData(XYLineAndShapeRenderer renderer) {
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesStroke(1, new BasicStroke(1.0f));
        renderer.setSeriesPaint(1, StatisticChartStyling.CORRELATIVE_POINT_PAINT);
        renderer.setSeriesFillPaint(1, StatisticChartStyling.CORRELATIVE_POINT_FILL_PAINT);
        renderer.setSeriesShape(1, StatisticChartStyling.CORRELATIVE_POINT_SHAPE);
    }


    @Override
    protected boolean mustHandleSelectionChange() {
        return super.mustHandleSelectionChange() || isVectorDataNodeChanged();
    }


    @Override
    protected void updateComponents() {
        if (!isInitialized || !isVisible()) {
            return;
        }

        final RasterDataNode raster = getRaster();
        if (raster != null) {
            chart.setTitle(CHART_TITLE + " for " + raster.getName());
        } else {
            chart.setTitle(CHART_TITLE);
        }

        correlativeFieldSelector.updatePointDataSource(getProduct());

        updateDataSource();
        updateDataSet();
        updateUIState();
        super.updateComponents();
    }

    private void updateDataSource() {
        if (!isInitialized) {
            return;
        }

        profileData = null;
        if (getRaster() != null) {
            try {
                if (dataSourceConfig.useCorrelativeData
                        && dataSourceConfig.pointDataSource != null) {
                    profileData = new TransectProfileDataBuilder()
                            .raster(getRaster())
                            .pointData(dataSourceConfig.pointDataSource)
                            .boxSize(dataSourceConfig.boxSize)
                            .connectVertices(dataSourceConfig.computeInBetweenPoints)
                            .useRoiMask(dataSourceConfig.useRoiMask)
                            .roiMask(dataSourceConfig.roiMask)
                            .build();
                } else {
                    Shape shape = StatisticsUtils.TransectProfile.getTransectShape(getRaster().getProduct());
                    if (shape != null) {
                        profileData = new TransectProfileDataBuilder()
                                .raster(getRaster())
                                .path(shape)
                                .boxSize(dataSourceConfig.boxSize)
                                .connectVertices(dataSourceConfig.computeInBetweenPoints)
                                .useRoiMask(dataSourceConfig.useRoiMask)
                                .roiMask(dataSourceConfig.roiMask)
                                .build();
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(getParent(),
                                              "Failed to compute profile plot.\n" +
                                                      "An I/O error occurred:" + e.getMessage(),
                                              "I/O error",
                                              JOptionPane.ERROR_MESSAGE);   /*I18N*/
            }
        }
    }


    private void updateDataSet() {
        if (!isInitialized) {
            return;
        }

        dataset.removeAllSeries();

        double dx = 0.5 * dataSourceConfig.boxSize;

        if (profileData != null) {
            final float[] sampleValues = profileData.getSampleValues();
            final float[] sampleSigmas = profileData.getSampleSigmas();
            XYIntervalSeries series = new XYIntervalSeries(getRaster() != null ? getRaster().getName() : DEFAULT_SAMPLE_DATASET_NAME);
            for (int x = 0; x < sampleValues.length; x++) {
                final float y = sampleValues[x];
                final float dy = sampleSigmas[x];
                series.add(x, x - dx, x + dx, y, y - dy, y + dy);
            }
            dataset.addSeries(series);

            if (dataSourceConfig.useCorrelativeData
                    && dataSourceConfig.pointDataSource != null
                    && dataSourceConfig.dataField != null) {

                XYIntervalSeries corrSeries = new XYIntervalSeries(getCorrelativeDataLabel(dataSourceConfig.pointDataSource, dataSourceConfig.dataField));
                int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
                SimpleFeature[] simpleFeatures = dataSourceConfig.pointDataSource.getFeatureCollection().toArray(new SimpleFeature[0]);

                if (shapeVertexIndexes.length == simpleFeatures.length) {
                    int fieldIndex = getAttributeIndex(dataSourceConfig.pointDataSource, dataSourceConfig.dataField);
                    if (fieldIndex != -1) {
                        for (int i = 0; i < simpleFeatures.length; i++) {
                            Number attribute = (Number) simpleFeatures[i].getAttribute(fieldIndex);
                            if (attribute != null) {
                                final double x = shapeVertexIndexes[i];
                                final double y = attribute.doubleValue();
                                corrSeries.add(x, x, x, y, y, y);
                            }
                        }
                        dataset.addSeries(corrSeries);
                    }
                } else {
                    System.out.println("Weird things happened:");
                    System.out.println("  shapeVertexIndexes.length = " + shapeVertexIndexes.length);
                    System.out.println("  simpleFeatures.length     = " + simpleFeatures.length);
                }

            }

            profilePlotDisplay.restoreAutoBounds();
            xAxisRangeControl.getBindingContext().setComponentsEnabled(PROPERTY_NAME_MARK_SEGMENTS,
                                                                       profileData.getShapeVertices().length > 2);
        }
    }

    private int getAttributeIndex(VectorDataNode pointDataSource, AttributeDescriptor dataField) {
        final String fieldName = dataField.getLocalName();
        if (fieldName.equals(CorrelativeFieldSelector.NULL_NAME)) {
            return -1;
        }
        return pointDataSource.getFeatureType().indexOf(fieldName);
    }

    private void updateUIState() {
        if (!isInitialized) {
            return;
        }

        xAxisRangeControl.getBindingContext().setComponentsEnabled(PROPERTY_NAME_MARK_SEGMENTS,
                                                                   profileData != null &&
                                                                           profileData.getShapeVertices().length > 2);
        xAxisRangeControl.setComponentsEnabled(profileData != null);
        yAxisRangeControl.setComponentsEnabled(profileData != null);
        adjustPlotAxes();

        if (dataSourceConfig.computeInBetweenPoints) {
            chart.getXYPlot().setRenderer(deviationRenderer);
        } else {
            chart.getXYPlot().setRenderer(pointRenderer);
        }

        chart.getXYPlot().getRangeAxis().setLabel(StatisticChartStyling.getAxisLabel(getRaster(), DEFAULT_SAMPLE_DATASET_NAME, false));

        boolean markSegments = (Boolean) (xAxisRangeControl.getBindingContext().getPropertySet().getValue(PROPERTY_NAME_MARK_SEGMENTS));
        if (markSegments && profileData != null && profileData.getNumShapeVertices() > 1) {
            final int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
            removeIntervalMarkers();
            for (int i = 0; i < shapeVertexIndexes.length - 1; i++) {
                if (i % 2 != 0) {
                    final IntervalMarker marker = new IntervalMarker(shapeVertexIndexes[i], shapeVertexIndexes[i + 1]);
                    marker.setPaint(new Color(120, 122, 125));
                    marker.setAlpha(0.3F);
                    chart.getXYPlot().addDomainMarker(marker, Layer.BACKGROUND);
                    intervalMarkers.add(marker);
                }
            }
        } else {
            removeIntervalMarkers();
        }

        pointDataSourceEnablement.apply();
        dataFieldEnablement.apply();

    }

    private void removeIntervalMarkers() {
        for (IntervalMarker intervalMarker : intervalMarkers) {
            chart.getXYPlot().removeDomainMarker(intervalMarker, Layer.BACKGROUND);
        }
        intervalMarkers.clear();
    }

    private void adjustAxisControlComponents() {
        if (!axisAdjusting) {
            axisAdjusting = true;
            try {
                if (xAxisRangeControl.isAutoMinMax()) {
                    xAxisRangeControl.adjustComponents(chart.getXYPlot().getDomainAxis(), 0);
                }
                if (yAxisRangeControl.isAutoMinMax()) {
                    yAxisRangeControl.adjustComponents(chart.getXYPlot().getRangeAxis(), 2);
                }
            } finally {
                axisAdjusting = false;
            }
        }
    }

    private void adjustPlotAxes() {
        if (!axisAdjusting) {
            axisAdjusting = true;
            try {
                xAxisRangeControl.adjustAxis(chart.getXYPlot().getDomainAxis(), 0);
                yAxisRangeControl.adjustAxis(chart.getXYPlot().getRangeAxis(), 2);
            } finally {
                axisAdjusting = false;
            }
        }
    }

    private void updateScalingOfYAxis() {
        final boolean logScaled = (Boolean) yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getPropertyValue();
        final XYPlot plot = chart.getXYPlot();
        plot.setRangeAxis(StatisticChartStyling.updateScalingOfAxis(logScaled, plot.getRangeAxis(), true));
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
        if (event.getSourceNode() instanceof VectorDataNode) {
            updateComponents();
        }
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
        if (event.getSourceNode() instanceof VectorDataNode) {
            updateComponents();
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        updateComponents();
    }

    @Override
    protected String getDataAsText() {
        if (profileData != null) {
            ProfileDataTableModel model = createProfileDataTableModel();
            return model.toCsv();
        } else {
            return "";
        }
    }

    private ProfileDataTableModel createProfileDataTableModel() {
        return new ProfileDataTableModel(getRaster().getName(), profileData, dataSourceConfig);
    }

    @Override
    public void handleLayerContentChanged() {
        updateComponents();
    }

    @SuppressWarnings("UnusedDeclaration")
    static class DataSourceConfig {
        int boxSize = 3;
        boolean useRoiMask;
        Mask roiMask;
        boolean computeInBetweenPoints = true;
        boolean useCorrelativeData;
        VectorDataNode pointDataSource;
        AttributeDescriptor dataField;
    }

    private class EnablePointDataCondition extends Enablement.Condition {

        @Override
        public boolean evaluate(BindingContext bindingContext) {
            return dataSourceConfig.useCorrelativeData && getProduct() != null;
        }
    }
}