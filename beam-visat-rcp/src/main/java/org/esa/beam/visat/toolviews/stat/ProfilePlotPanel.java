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
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The profile plot pane within the statistics window.
 */
class ProfilePlotPanel extends PagePanel {

    private static final String CHART_TITLE = "Profile Plot";
    private static final String TITLE_PREFIX = CHART_TITLE;
    private static final String NO_DATA_MESSAGE = "No profile plot computed yet. " +
                                                  "It will be computed if a geometry is selected within the image view.\n" +
                                                  ZOOM_TIP_MESSAGE;

    public static final String PROPERTY_NAME_MARK_SEGMENTS = "markSegments";
    public static final String PROPERTY_NAME_LOG_SCALED = "logScaled";

    private AxisRangeControl xAxisRangeControl;
    private AxisRangeControl yAxisRangeControl;

    private static boolean isInitialized = false;

    private ChartPanel profilePlotDisplay;
    private JFreeChart chart;
    private XYIntervalSeriesCollection dataset;
    private TransectProfileData profileData;

    private boolean axisAdjusting = false;

    private Set<IntervalMarker> intervalMarkers;
    private CorrelativeFieldSelector correlativeFieldSelector;
    private DataSourceConfig dataSourceConfig;
    private RoiMaskSelector roiMaskSelector;
    private DeviationRenderer deviationRenderer;
    private XYErrorRenderer pointRenderer;
    private Enablement pointDataSourceEnablement;
    private Enablement dataFieldEnablement;

    ProfilePlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId);

    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
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

        dataset = new XYIntervalSeriesCollection();
        chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                "Path (pixel)",
                "Sample value",
                dataset,
                PlotOrientation.VERTICAL,
                false, // Legend?
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
        deviationRenderer.setSeriesPaint(0, new Color(0, 0, 200));
        deviationRenderer.setSeriesFillPaint(0, new Color(150, 150, 255));

        pointRenderer = new XYErrorRenderer();
        pointRenderer.setUseFillPaint(true);
        pointRenderer.setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        pointRenderer.setSeriesLinesVisible(0, false);
        pointRenderer.setSeriesShapesVisible(0, true);
        pointRenderer.setSeriesStroke(0, new BasicStroke(1.0f));
        pointRenderer.setSeriesPaint(0, new Color(0, 0, 200));
        pointRenderer.setSeriesFillPaint(0, new Color(150, 150, 255));
        pointRenderer.setSeriesShape(0, new Ellipse2D.Float(-4, -4, 8, 8));

        configureRendererForCorrelativeData(deviationRenderer);
        configureRendererForCorrelativeData(pointRenderer);

        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setRenderer(deviationRenderer);

        profilePlotDisplay = new ChartPanel(chart);
        profilePlotDisplay.setInitialDelay(200);
        profilePlotDisplay.setDismissDelay(1500);
        profilePlotDisplay.setReshowDelay(200);
        profilePlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        final AxisChangeListener axisListener = new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent event) {
                adjustAxisControlComponents();
            }
        };
        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();
        domainAxis.addChangeListener(axisListener);
        rangeAxis.addChangeListener(axisListener);


        final JLabel boxSizeLabel = new JLabel("Box size: ");
        final JSpinner boxSizeSpinner = new JSpinner();
        final JCheckBox computeInBetweenPoints = new JCheckBox("Compute in-between points");
        final JCheckBox useCorrelativeData = new JCheckBox("Use correlative data");

        dataSourceConfig = new DataSourceConfig();
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        roiMaskSelector = new RoiMaskSelector(bindingContext);

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
        GridBagUtils.addToPanel(dataSourceOptionsPanel, boxSizeLabel, dataSourceOptionsConstraints, "gridwidth=1,gridy=0,gridx=0,weightx=0");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, boxSizeSpinner, dataSourceOptionsConstraints, "gridwidth=1,gridy=0,gridx=1,weightx=1");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, roiMaskSelector.createPanel(), dataSourceOptionsConstraints, "gridwidth=2,gridy=1,gridx=0");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, computeInBetweenPoints, dataSourceOptionsConstraints, "gridy=3");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, new JLabel(" "), dataSourceOptionsConstraints, "gridy=4");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, useCorrelativeData, dataSourceOptionsConstraints, "gridy=5");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.pointDataSourceLabel, dataSourceOptionsConstraints, "gridy=6");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.pointDataSourceList, dataSourceOptionsConstraints, "gridy=7");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.dataFieldLabel, dataSourceOptionsConstraints, "gridy=8");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, correlativeFieldSelector.dataFieldList, dataSourceOptionsConstraints, "gridy=9");

        xAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_MARK_SEGMENTS, new JCheckBox("Mark segments"));
        yAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_LOG_SCALED, new JCheckBox("Log scaled"));

        JPanel displayOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints displayOptionsConstraints = GridBagUtils.createConstraints("anchor=SOUTH,fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=0");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_MARK_SEGMENTS).getComponents()[0], displayOptionsConstraints, "gridy=1");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getPanel(), displayOptionsConstraints, "gridy=2");
        GridBagUtils.addToPanel(displayOptionsPanel, yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getComponents()[0], displayOptionsConstraints, "gridy=3");

        JPanel rightPanel = GridBagUtils.createPanel();
        GridBagConstraints rightPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(rightPanel, dataSourceOptionsPanel, rightPanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(rightPanel, new JPanel(), rightPanelConstraints, "gridy=1,fill=VERTICAL,weighty=1");
        GridBagUtils.addToPanel(rightPanel, displayOptionsPanel, rightPanelConstraints, "gridy=2,fill=HORIZONTAL,weighty=0");
        GridBagUtils.addToPanel(rightPanel, new JSeparator(), rightPanelConstraints, "gridy=3");
        GridBagUtils.addToPanel(rightPanel, createChartButtonPanel2(profilePlotDisplay), rightPanelConstraints, "gridy=4");

        add(profilePlotDisplay, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        isInitialized = true;
        updateContent();
    }

    private void configureRendererForCorrelativeData(XYLineAndShapeRenderer renderer) {
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesStroke(1, new BasicStroke(1.0f));
        renderer.setSeriesPaint(1, new Color(200, 0, 0));
        renderer.setSeriesFillPaint(1, new Color(255, 150, 150));
        renderer.setSeriesShape(1, new Ellipse2D.Float(-4, -4, 8, 8));
    }


    @Override
    protected boolean mustUpdateContent() {
        return super.mustUpdateContent() || isVectorDataNodeChanged();
    }


    @Override
    protected void updateContent() {
        if (!isInitialized) {
            return;
        }

        if (getRaster() != null) {
            chart.setTitle(CHART_TITLE + " for " + getRaster().getName());
        } else {
            chart.setTitle(CHART_TITLE);
        }

        correlativeFieldSelector.updatePointDataSource(getProduct());

        updateDataSource();
        updateDataSet();
        updateUIState();
        roiMaskSelector.updateMaskSource(getProduct());
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
                    profileData = TransectProfileData.create(getRaster(), dataSourceConfig.pointDataSource, dataSourceConfig.boxSize, dataSourceConfig.useRoiMask, dataSourceConfig.roiMask, dataSourceConfig.computeInBetweenPoints);
                } else {
                    Shape shape = StatisticsUtils.TransectProfile.getTransectShape(getRaster().getProduct());
                    if (shape != null) {
                        profileData = TransectProfileData.create(getRaster(), shape, dataSourceConfig.boxSize, dataSourceConfig.useRoiMask, dataSourceConfig.roiMask, dataSourceConfig.computeInBetweenPoints);
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
            XYIntervalSeries series = new XYIntervalSeries("Sample Values");
            for (int x = 0; x < sampleValues.length; x++) {
                final float y = sampleValues[x];
                final float dy = sampleSigmas[x];
                series.add(x, x - dx, x + dx, y, y - dy, y + dy);
            }
            dataset.addSeries(series);

            if (dataSourceConfig.useCorrelativeData
                && dataSourceConfig.pointDataSource != null
                && dataSourceConfig.dataField != null) {

                XYIntervalSeries corrSeries = new XYIntervalSeries("Correlative Values");
                int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
                SimpleFeature[] simpleFeatures = dataSourceConfig.pointDataSource.getFeatureCollection().toArray(new SimpleFeature[0]);

                if (shapeVertexIndexes.length == simpleFeatures.length) {
                    String fieldName = dataSourceConfig.dataField.getLocalName();
                    for (int i = 0; i < simpleFeatures.length; i++) {
                        Number attribute = (Number) simpleFeatures[i].getAttribute(fieldName);
                        final double x = shapeVertexIndexes[i];
                        final double y = attribute.doubleValue();
                        corrSeries.add(x, x, x, y, y, y);
                    }
                } else {
                    System.out.println("Weird things happened:");
                    System.out.println("  shapeVertexIndexes.length = " + shapeVertexIndexes.length);
                    System.out.println("  simpleFeatures.length     = " + simpleFeatures.length);
                }

                dataset.addSeries(corrSeries);
            }

            profilePlotDisplay.restoreAutoBounds();
            xAxisRangeControl.getBindingContext().setComponentsEnabled(PROPERTY_NAME_MARK_SEGMENTS,
                                                                       profileData.getShapeVertices().length > 2);
        }
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
        if ((Boolean) yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getPropertyValue()) {
            ValueAxis oldAxis = chart.getXYPlot().getRangeAxis();
            if (!(oldAxis instanceof LogarithmicAxis)) {
                LogarithmicAxis logAxisX = new LogarithmicAxis(oldAxis.getLabel());
                logAxisX.setAllowNegativesFlag(true);
                logAxisX.setLog10TickLabelsFlag(true);
                logAxisX.setMinorTickCount(10);
                chart.getXYPlot().setRangeAxis(logAxisX);
            }
        } else {
            ValueAxis oldAxis = chart.getXYPlot().getRangeAxis();
            if (oldAxis instanceof LogarithmicAxis) {
                NumberAxis xAxis = new NumberAxis(oldAxis.getLabel());
                chart.getXYPlot().setRangeAxis(xAxis);
            }
        }
    }


    @Override
    protected String getDataAsText() {
        try {
            return StatisticsUtils.TransectProfile.createTransectProfileText(getRaster());
        } catch (IOException ignore) {
            return "";
        }
    }

    @Override
    public void handleLayerContentChanged() {
        updateContent();
    }

    @Override
    public void handleViewSelectionChanged() {
        updateContent();
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class DataSourceConfig {

        private int boxSize = 3;
        public boolean useRoiMask;
        private Mask roiMask;
        private boolean computeInBetweenPoints = true;
        private boolean useCorrelativeData;
        private VectorDataNode pointDataSource;
        private AttributeDescriptor dataField;
    }

    private class EnablePointDataCondition extends Enablement.Condition {

        @Override
        public boolean evaluate(BindingContext bindingContext) {
            return dataSourceConfig.useCorrelativeData && getProduct() != null;
        }
    }
}