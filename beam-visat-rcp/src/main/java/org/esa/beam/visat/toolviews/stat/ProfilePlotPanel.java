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
import com.bc.ceres.swing.binding.BindingContext;
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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

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
    private YIntervalSeriesCollection dataset;
    private TransectProfileData profileData;

    private boolean axisAdjusting = false;
    private CorrelativeFieldSelector correlativeFieldSelector;
    private DataSourceConfig dataSourceConfig;

    ProfilePlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId);

    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
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

        dataset = new YIntervalSeriesCollection();
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

        DeviationRenderer renderer = new DeviationRenderer();
        renderer.setUseFillPaint(true);
        renderer.setBaseToolTipGenerator(new XYPlotToolTipGenerator());

        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesPaint(0, new Color(0, 0, 200));
        renderer.setSeriesFillPaint(0, new Color(150, 150, 255));

        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesStroke(1, new BasicStroke(1.0f));
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesFillPaint(1, Color.white);
        renderer.setSeriesShape(1, new Ellipse2D.Float(-4, -4, 8, 8));

        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setRenderer(renderer);

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
        final JLabel roiMaskLabel = new JLabel("ROI mask: ");
        final JComboBox roiMaskList = new JComboBox();
        final JButton roiMaskButton = new JButton("..."); // todo - use action from mask manager
        final JCheckBox computeInBetweenPoints = new JCheckBox("Compute in-between points");
        final JCheckBox useCorrelativeData = new JCheckBox("Use correlative data");

        dataSourceConfig = new DataSourceConfig();
        final BindingContext bindingContext = new BindingContext(PropertyContainer.createObjectBacked(dataSourceConfig));

        correlativeFieldSelector = new CorrelativeFieldSelector(bindingContext);
        final PropertyDescriptor boxSizeDescriptor = bindingContext.getPropertySet().getProperty("boxSize").getDescriptor();
        boxSizeDescriptor.setValueRange(new ValueRange(1, 101));
        boxSizeDescriptor.setAttribute("stepSize", 2);
        boxSizeDescriptor.setValidator(new Validator() {
            @Override
            public void validateValue(Property property, Object value) throws ValidationException {
                if (((Number) value).intValue() % 2 == 0) {
                    throw new ValidationException("Only uneven values allowed as box size.");
                }
            }
        });
        bindingContext.bind("boxSize", boxSizeSpinner);
        bindingContext.bind("roiMask", roiMaskList);
        bindingContext.getBinding("roiMask").addComponent(roiMaskLabel);
        bindingContext.getBinding("roiMask").addComponent(roiMaskButton);
        bindingContext.bind("computeInBetweenPoints", computeInBetweenPoints);
        bindingContext.bind("useCorrelativeData", useCorrelativeData);
        bindingContext.bindEnabledState("roiMask", true, new RoiMaskEnabledCondition());
        bindingContext.bindEnabledState("pointDataSource", true, "useCorrelativeData", true);
        bindingContext.bindEnabledState("dataField", true, "useCorrelativeData", true);

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
        GridBagUtils.addToPanel(dataSourceOptionsPanel, roiMaskLabel, dataSourceOptionsConstraints, "gridwidth=2,gridy=1,gridx=0");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, roiMaskList, dataSourceOptionsConstraints, "gridy=2");
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
                    profileData = TransectProfileData.create(getRaster(), dataSourceConfig.pointDataSource, dataSourceConfig.boxSize, null);
                } else {
                    Shape shape = StatisticsUtils.TransectProfile.getTransectShape(getRaster().getProduct());
                    if (shape != null) {
                        profileData = TransectProfileData.create(getRaster(), shape, dataSourceConfig.boxSize, null);
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

        if (profileData != null && profileData.getNumShapeVertices() >= 2) {
            final float[] sampleValues = profileData.getSampleValues();
            final float[] sampleSigmas = profileData.getSampleSigmas();
            boolean markSegments = (Boolean) (xAxisRangeControl.getBindingContext().getPropertySet().getValue(PROPERTY_NAME_MARK_SEGMENTS));
            if (profileData.getNumShapeVertices() == 2 || !markSegments) {
                YIntervalSeries series = new YIntervalSeries("Sample Values");
                for (int i = 0; i < sampleValues.length; i++) {
                    series.add(i, sampleValues[i], sampleValues[i] - sampleSigmas[i], sampleValues[i] + sampleSigmas[i]);
                }
                dataset.addSeries(series);
            } else {
                for (int i = 0; i < profileData.getNumShapeVertices() - 1; i++) {
                    final YIntervalSeries series = new YIntervalSeries(String.format("Sample Values Segment %d", i));
                    for (int x = profileData.getShapeVertexIndexes()[i]; x <= profileData.getShapeVertexIndexes()[i + 1]; x++) {
                        series.add(i, sampleValues[x], sampleValues[i] - sampleSigmas[i], sampleValues[i] + sampleSigmas[i]);
                    }
                    dataset.addSeries(series);
                }
            }

            if (dataSourceConfig.useCorrelativeData
                    && dataSourceConfig.pointDataSource != null
                    && dataSourceConfig.dataField != null) {

                YIntervalSeries corrSeries = new YIntervalSeries("Correlative Values");
                int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
                SimpleFeature[] simpleFeatures = dataSourceConfig.pointDataSource.getFeatureCollection().toArray(new SimpleFeature[0]);

                if (shapeVertexIndexes.length == simpleFeatures.length) {
                    String fieldName = dataSourceConfig.dataField.getLocalName();
                    for (int i = 0; i < simpleFeatures.length; i++) {
                        Number attribute = (Number) simpleFeatures[i].getAttribute(fieldName);
                        final double y0 = attribute.doubleValue();
                        corrSeries.add(shapeVertexIndexes[i], y0, y0, y0);
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
                                                                   profileData != null && profileData.getShapeVertices().length > 2);
        xAxisRangeControl.setComponentsEnabled(profileData != null);
        yAxisRangeControl.setComponentsEnabled(profileData != null);
        adjustPlotAxes();
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

    private static class DataSourceConfig {
        private int boxSize = 3;
        private String roiMask;
        private boolean computeInBetweenPoints = true;
        private boolean useCorrelativeData;
        private VectorDataNode pointDataSource;
        private AttributeDescriptor dataField;
    }

    private static class RoiMaskEnabledCondition extends BindingContext.Condition {
        @Override
        public boolean evaluate(BindingContext bindingContext) {
            return true; // todo - be smarter
        }

        @Override
        public void addPropertyChangeListener(BindingContext bindingContext, PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(BindingContext bindingContext, PropertyChangeListener listener) {
        }
    }
}