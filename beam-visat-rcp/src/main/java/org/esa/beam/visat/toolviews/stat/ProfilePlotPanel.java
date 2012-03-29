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
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.TransectProfileData;
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
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * The profile plot pane within the statistcs window.
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
    private XYSeriesCollection dataset;
    private TransectProfileData profileData;

    private boolean axisAdjusting = false;

    ProfilePlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId);

    }

    // move to BindingContext?
    public static void setComponentsEnabled(BindingContext bindingContext, String propertyName, boolean enabled) {
        final JComponent[] components = bindingContext.getBinding(propertyName).getComponents();
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
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

        dataset = new XYSeriesCollection();
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

        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.getRenderer().setBaseToolTipGenerator(new XYPlotToolTipGenerator());
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

        final JPanel optionsPanel = GridBagUtils.createPanel();

        xAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_MARK_SEGMENTS, new JCheckBox("Mark segments"));
        yAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_LOG_SCALED, new JCheckBox("Log scaled"));

        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=BOTH");
        GridBagUtils.addToPanel(optionsPanel, xAxisRangeControl.getPanel(), gbc, "gridy=1");
        GridBagUtils.addToPanel(optionsPanel, xAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_MARK_SEGMENTS).getComponents()[0], gbc, "gridy=2,insets=2");
        GridBagUtils.addToPanel(optionsPanel, yAxisRangeControl.getPanel(), gbc, "gridy=4");
        GridBagUtils.addToPanel(optionsPanel, yAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED).getComponents()[0], gbc, "gridy=5");

        final TableLayout rightPanelLayout = new TableLayout(1);
        final JPanel rightPanel = new JPanel(rightPanelLayout);
        rightPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        rightPanelLayout.setRowWeightY(2, 1.0);
        rightPanelLayout.setRowAnchor(3, TableLayout.Anchor.EAST);
        rightPanel.add(optionsPanel);
        rightPanel.add(createChartButtonPanel(profilePlotDisplay));
        rightPanel.add(new JPanel());   // filler
        final JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(getHelpButton(), BorderLayout.EAST);
        rightPanel.add(helpPanel);

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
        try {
            profileData = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occurred:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);   /*I18N*/
            return;
        }
        chart.setTitle(getRaster() != null ? CHART_TITLE + " for " + getRaster().getName() : CHART_TITLE);
        updateDataSet();
        updateUIState();
    }

    private void updateDataSet() {
        dataset.removeAllSeries();
        if (profileData != null) {
            final float[] sampleValues = profileData.getSampleValues();
            if (profileData.getNumShapeVertices() <= 2 || !(Boolean) (xAxisRangeControl.getBindingContext().getPropertySet().getValue(PROPERTY_NAME_MARK_SEGMENTS))) {
                XYSeries series = new XYSeries("Sample Values");
                for (int i = 0; i < sampleValues.length; i++) {
                    series.add(i, sampleValues[i]);
                }
                dataset.addSeries(series);
            } else {
                for (int i = 0; i < profileData.getNumShapeVertices() - 1; i++) {
                    final XYSeries series = new XYSeries(String.format("Sample Values Segment %d", i));
                    for (int x = profileData.getShapeVertexIndexes()[i]; x <= profileData.getShapeVertexIndexes()[i + 1]; x++) {
                        series.add(x, sampleValues[x]);
                    }
                    dataset.addSeries(series);
                }
            }
            profilePlotDisplay.restoreAutoBounds();
            setComponentsEnabled(xAxisRangeControl.getBindingContext(),
                                 PROPERTY_NAME_MARK_SEGMENTS, profileData.getShapeVertices().length > 2);
        }
    }

    private void updateUIState() {
        if (!isInitialized) {
            return;
        }
        setComponentsEnabled(xAxisRangeControl.getBindingContext(),
                             PROPERTY_NAME_MARK_SEGMENTS,
                             profileData != null && profileData.getShapeVertices().length > 2);
        xAxisRangeControl.setEnabled(profileData != null);
        yAxisRangeControl.setEnabled(profileData != null);
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

}
