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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.validators.NumberValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.io.IOException;

/**
 * The profile plot pane within the statistcs window.
 *
 */
class ProfilePlotPanel extends PagePanel {

    private static final String CHART_TITLE = "Profile Plot";
    private static final String TITLE_PREFIX = CHART_TITLE;
    private static final String NO_DATA_MESSAGE = "No profile plot computed yet. " +
                                                  "It will be computed if a geometry is selected within the image view.\n" +
                                                  ZOOM_TIP_MESSAGE;  

    private static final int VAR1 = 0;
    private static final int VAR2 = 1;

    private static final Parameter[] minParams = new Parameter[2];
    private static final Parameter[] maxParams = new Parameter[2];
    private Parameter markSegmentsParam;
    private static boolean isInitialized = false;

    private ParamGroup paramGroup;
    private ChartPanel profilePlotDisplay;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private TransectProfileData profileData;

    private boolean axisAdjusting = false;

    ProfilePlotPanel(ToolView parentDialog, String helpId) {
        super(parentDialog, helpId);
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
            if (profileData.getNumShapeVertices() <= 2 || !(Boolean) markSegmentsParam.getValue()) {
                XYSeries series = new XYSeries("Sample Values");
                for (int i = 0; i < sampleValues.length; i++) {
                    series.add(i, sampleValues[i]);
                }
                dataset.addSeries(series);
            } else {
                final XYSeries[] xySerieses = new XYSeries[profileData.getNumShapeVertices() - 1];
                for (int i = 0; i < xySerieses.length; i++) {
                    final XYSeries series = new XYSeries(String.format("Sample Values Segment %d", i));
                    for (int x = profileData.getShapeVertexIndexes()[i]; x <= profileData.getShapeVertexIndexes()[i + 1]; x++) {
                        series.add(x, sampleValues[x]);
                    }
                    dataset.addSeries(series);
                }
            }
            profilePlotDisplay.restoreAutoBounds();
            markSegmentsParam.setUIEnabled(profileData.getShapeVertices().length > 2);
        }
    }


    private void initParameters() {
        paramGroup = new ParamGroup();
        initParameters(VAR1);
        initParameters(VAR2);
        paramGroup.addParamChangeListener(new ParamChangeListener() {

            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                if(event.getParameter().equals(markSegmentsParam)) {
                   updateDataSet();
                }
                updateUIState();
            }
        });
    }

    private void initParameters(int varIndex) {

        final String paramPrefix = "var" + varIndex + ".";
        final String axis = (varIndex == VAR1) ? "X-Axis" : "Y-Axis";

        Object paramValue = varIndex == VAR1 ? Integer.valueOf(0) : new Float(0.0f);
        minParams[varIndex] = new Parameter(paramPrefix + "min", paramValue);
        minParams[varIndex].getProperties().setLabel("Min:");
        minParams[varIndex].getProperties().setDescription("Minimum display value for " + axis);    /*I18N*/
        minParams[varIndex].getProperties().setNumCols(7);
        if (varIndex == VAR1) {
            minParams[varIndex].getProperties().setValidatorClass(NumberValidator.class);
        }
        paramGroup.addParameter(minParams[varIndex]);

        paramValue = varIndex == VAR1 ? Integer.valueOf(100) : new Float(100.0f);
        maxParams[varIndex] = new Parameter(paramPrefix + "max", paramValue);
        maxParams[varIndex].getProperties().setLabel("Max:");
        maxParams[varIndex].getProperties().setDescription("Maximum display value for " + axis);    /*I18N*/
        maxParams[varIndex].getProperties().setNumCols(7);
        if (varIndex == VAR1) {
            maxParams[varIndex].getProperties().setValidatorClass(NumberValidator.class);
        }
        paramGroup.addParameter(maxParams[varIndex]);

        if (varIndex == VAR1) {
            markSegmentsParam = new Parameter(paramPrefix + "markSegments", false);
            markSegmentsParam.getProperties().setLabel("Mark segments");
            markSegmentsParam.getProperties().setDescription("Toggle whether or not to mark segements");    /*I18N*/
            markSegmentsParam.setValue(false, null);
            paramGroup.addParameter(markSegmentsParam);
        }
    }

    private void createUI() {

        dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart(
                CHART_TITLE,
                "Way (pixel)",
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
                adjustAxisParameter();
            }
        };
        final ValueAxis domainAxis = plot.getDomainAxis();
        final ValueAxis rangeAxis = plot.getRangeAxis();
        domainAxis.addChangeListener(axisListener);
        rangeAxis.addChangeListener(axisListener);

        final TableLayout rightPanelLayout = new TableLayout(1);
        final JPanel rightPanel = new JPanel(rightPanelLayout);
        rightPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        rightPanelLayout.setRowWeightY(2, 1.0);
        rightPanelLayout.setRowAnchor(3, TableLayout.Anchor.EAST);
        rightPanel.add(createOptionsPane());
        rightPanel.add(createChartButtonPanel(profilePlotDisplay));
        rightPanel.add(new JPanel());   // filler
        final JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(getHelpButton(), BorderLayout.EAST);
        rightPanel.add(helpPanel);
        
        add(profilePlotDisplay, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        isInitialized = true;

    }

    private void updateUIState() {
        if (!isInitialized) {
            return;
        }
        markSegmentsParam.setUIEnabled(profileData != null && profileData.getShapeVertices().length > 2);
        updateParamState(VAR1);
        updateParamState(VAR2);
        adjustAxis();
    }

    private void adjustAxisParameter() {
        if (!axisAdjusting) {
            axisAdjusting = true;
            try {
                final ValueAxis domainAxis = chart.getXYPlot().getDomainAxis();
                minParams[VAR1].setValue((int) Math.floor(domainAxis.getLowerBound()), null);
                maxParams[VAR1].setValue((int) Math.ceil(domainAxis.getUpperBound()), null);

                final ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
                minParams[VAR2].setValueAsText(String.format("%7.2f", rangeAxis.getLowerBound()), null);
                maxParams[VAR2].setValueAsText(String.format("%7.2f", rangeAxis.getUpperBound()), null);
            } finally {
                axisAdjusting = false;
            }
        }
    }

    private void adjustAxis() {
        if (!axisAdjusting) {
            axisAdjusting = true;
            try {
                final ValueAxis domainAxis = chart.getXYPlot().getDomainAxis();
                final int lowerDomain = ((Number) minParams[VAR1].getValue()).intValue();
                final int upperDomain = ((Number) maxParams[VAR1].getValue()).intValue();
                domainAxis.setRange(lowerDomain, upperDomain);

                final ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
                final float lowerRange = ((Number) minParams[VAR2].getValue()).floatValue();
                final float upperRnge = ((Number) maxParams[VAR2].getValue()).floatValue();
                rangeAxis.setRange(lowerRange, upperRnge);
            } finally {
                axisAdjusting = false;
            }
        }
    }

    private void updateParamState(int varIndex) {
        if (!isInitialized) {
            return;
        }
        minParams[varIndex].setUIEnabled(profileData != null);
        maxParams[varIndex].setUIEnabled(profileData != null);
    }


    private JPanel createOptionsPane() {
        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=BOTH");

        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR1), gbc, "gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR2), gbc, "gridy=1,insets.top=7,weightx=1");
        return optionsPane;
    }

    private JPanel createOptionsPane(int varIndex) {

        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=1,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, minParams[varIndex].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, minParams[varIndex].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, maxParams[varIndex].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, maxParams[varIndex].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        if (varIndex == VAR1) {
            GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=3,insets.top=4");
            GridBagUtils.addToPanel(optionsPane, markSegmentsParam.getEditor().getComponent(), gbc,
                                    "gridx=0,weightx=0");
        }

        optionsPane.setBorder(BorderFactory.createTitledBorder(varIndex == 0 ? "X-Axis" : "Y-Axis"));

        return optionsPane;
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
