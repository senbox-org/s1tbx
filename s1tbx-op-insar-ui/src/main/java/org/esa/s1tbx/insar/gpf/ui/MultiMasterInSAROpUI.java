/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.insar.gpf.ui;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.AppContext;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User interface for MultiReferenceInSAROp
 */
public class MultiMasterInSAROpUI extends BaseOperatorUI {
    // Components related to operator's input parameters
    private final JLabel orbitDegreeLabel = new JLabel("Orbit interpolation degree");
    private final JComboBox<Integer> orbitDegreeComboBox = new JComboBox(new Integer[]{1, 2, 3, 4, 5});

    private final JCheckBox includeWavenumberCheckBox = new JCheckBox("Include wavenumber");
    private final JCheckBox includeIncidenceAngleCheckBox = new JCheckBox("Include incidence angle");
    private final JCheckBox includeLatLonCheckBox = new JCheckBox("Include latitude and longitude");

    private final JLabel cohWindowAzLabel = new JLabel("Coherence azimuth window size");
    private final JLabel cohWindowRgLabel = new JLabel("Coherence range window size");
    private final JTextField cohWindowAzTextField = new JTextField("");
    private final JTextField cohWindowRgTextField = new JTextField("");

    // Helper components
    private final JLabel maxDopplerCentroidDiffLabel = new JLabel("Max doppler centroid diff [Hz]");
    private final JLabel maxSpatialBaselineDiffLabel = new JLabel("Max spatial baseline diff [m]");
    private final JLabel maxTemporalBaselineDiffLabel = new JLabel("Max temporal baseline diff [days]");
    private JSlider maxDopplerCentroidDiffSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 500);
    private JSlider maxSpatialBaselineDiffSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 500);
    private JSlider maxTemporalBaselineDiffSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 500);

    private final JRadioButton dopplerCentroidRadioButton = new JRadioButton("Doppler centroid");
    private final JRadioButton spatialBaselineRadioButton = new JRadioButton("Spatial baseline", true);

    private final JButton savePlotButton = new JButton("Save plot");
    private ChartPanel chartPanel;

    // Helper attributes
    private Boolean includeWavenumber = false;
    private Boolean includeIncidenceAngle = false;
    private Boolean includeLatLon = false;

    private String[] pairs;

    private Boolean skipDrawChart = true;
    private Boolean defaultSliderDataLoaded = false;
    private int[][] minMaxSelectedSliderData;
    private String yAxisForPlot = "Spatial baseline [m]";

    // Constants
    private static final int NUMBER_OF_PAIRS_TO_WARN = 50;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        skipDrawChart = true;
        final int[] sliderValue = new int[3];
        maxTemporalBaselineDiffSlider.addChangeListener(e -> {
            if (maxTemporalBaselineDiffSlider.getValueIsAdjusting()) {
                return;
            }
            sliderValue[0] = maxTemporalBaselineDiffSlider.getValue();
            if (!skipDrawChart) {
                chartPanel.setChart(getPairsAndDrawChart(sliderValue, false));
            }
        });
        maxDopplerCentroidDiffSlider.addChangeListener(e -> {
            if (maxDopplerCentroidDiffSlider.getValueIsAdjusting()) {
                return;
            }
            sliderValue[1] = maxDopplerCentroidDiffSlider.getValue();
            if (!skipDrawChart) {
                chartPanel.setChart(getPairsAndDrawChart(sliderValue, false));
            }
        });
        maxSpatialBaselineDiffSlider.addChangeListener(e -> {
            if (maxSpatialBaselineDiffSlider.getValueIsAdjusting()) {
                return;
            }
            sliderValue[2] = maxSpatialBaselineDiffSlider.getValue();
            if (!skipDrawChart) {
                chartPanel.setChart(getPairsAndDrawChart(sliderValue, false));
            }
        });

        dopplerCentroidRadioButton.addItemListener(e -> {
            final boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected) {
                yAxisForPlot = "Doppler centroid [Hz]";
                spatialBaselineRadioButton.setSelected(false);
            } else {
                spatialBaselineRadioButton.setSelected(true);
            }
            chartPanel.setChart(getPairsAndDrawChart(sliderValue, false));
        });
        spatialBaselineRadioButton.addItemListener(e -> {
            final boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected) {
                yAxisForPlot = "Spatial baseline [m]";
                dopplerCentroidRadioButton.setSelected(false);
            } else {
                dopplerCentroidRadioButton.setSelected(true);
            }
            chartPanel.setChart(getPairsAndDrawChart(sliderValue, false));
        });

        savePlotButton.addActionListener(e -> {
            chartPanel.setChart(getPairsAndDrawChart(sliderValue, true));
            try {
                chartPanel.doSaveAs();
            } catch (IOException ioe) {
                AbstractDialog.showErrorDialog(chartPanel, "Could not save plot:\n"
                        + ioe.getMessage(), "Error");
            }
        });

        includeWavenumberCheckBox.addItemListener(e -> includeWavenumber = (e.getStateChange() == ItemEvent.SELECTED));
        includeIncidenceAngleCheckBox.addItemListener(e -> includeIncidenceAngle = (e.getStateChange() == ItemEvent.SELECTED));
        includeLatLonCheckBox.addItemListener(e -> includeLatLon = (e.getStateChange() == ItemEvent.SELECTED));

        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        orbitDegreeComboBox.setSelectedItem(paramMap.get("orbitDegree"));

        includeWavenumberCheckBox.setSelected(includeWavenumber);
        includeIncidenceAngleCheckBox.setSelected(includeIncidenceAngle);
        includeLatLonCheckBox.setSelected(includeLatLon);

        cohWindowAzTextField.setText(String.valueOf(paramMap.get("cohWindowAz")));
        cohWindowRgTextField.setText(String.valueOf(paramMap.get("cohWindowRg")));

        if (hasSourceProducts()) {
            minMaxSelectedSliderData = getMinMaxMean();

            skipDrawChart = true;
            maxTemporalBaselineDiffSlider.setMinimum(minMaxSelectedSliderData[0][0]);
            maxTemporalBaselineDiffSlider.setMaximum(minMaxSelectedSliderData[0][1]);

            maxDopplerCentroidDiffSlider.setMinimum(minMaxSelectedSliderData[1][0]);
            maxDopplerCentroidDiffSlider.setMaximum(minMaxSelectedSliderData[1][1]);

            maxSpatialBaselineDiffSlider.setMinimum(minMaxSelectedSliderData[2][0]);
            skipDrawChart = false;
            maxSpatialBaselineDiffSlider.setMaximum(minMaxSelectedSliderData[2][1]);

            if (!defaultSliderDataLoaded) { // if this is the first time initParameters is called
                skipDrawChart = true;
                maxTemporalBaselineDiffSlider.setValue(3 * minMaxSelectedSliderData[0][0]);
                maxDopplerCentroidDiffSlider.setValue(minMaxSelectedSliderData[1][2]);
                skipDrawChart = false;
                maxSpatialBaselineDiffSlider.setValue(minMaxSelectedSliderData[2][2]);

                defaultSliderDataLoaded = true;
            }
        }
    }

    @Override
    public UIValidation validateParameters() {

        if (!hasSourceProducts()) {
            return new UIValidation(UIValidation.State.OK, "");
        }

        if (pairs != null && pairs.length > 0) {
            if (pairs.length > NUMBER_OF_PAIRS_TO_WARN) {
                return new UIValidation(UIValidation.State.WARNING,
                                        String.format("You are about to generate %d InSAR pairs. This will" +
                                                              " result in a large number of bands.", pairs.length));
            } else {
                return new UIValidation(UIValidation.State.OK, "");
            }
        }

        return new UIValidation(UIValidation.State.ERROR,
                                "No InSAR pairs found. Check sliders.");
    }

    @Override
    public void updateParameters() {

        paramMap.put("orbitDegree", orbitDegreeComboBox.getSelectedItem());

        paramMap.put("pairs", pairs);

        paramMap.put("includeWavenumber", includeWavenumber);
        paramMap.put("includeIncidenceAngle", includeIncidenceAngle);
        paramMap.put("includeLatLon", includeLatLon);

        final String cohWindowAzStr = cohWindowAzTextField.getText();
        final String cohWindowRgStr = cohWindowRgTextField.getText();

        if (cohWindowAzStr != null && !cohWindowAzStr.isEmpty()) {
            paramMap.put("cohWindowAz", Integer.parseInt(cohWindowAzStr));
        }
        if (cohWindowRgStr != null && !cohWindowRgStr.isEmpty()) {
            paramMap.put("cohWindowRg", Integer.parseInt(cohWindowRgStr));
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        contentPane.setBorder(BorderFactory.createTitledBorder("Configuration parameters:"));

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, orbitDegreeLabel, orbitDegreeComboBox);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWindowRgLabel, cohWindowRgTextField);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWindowAzLabel, cohWindowAzTextField);

        gbc.gridy++;
        contentPane.add(includeLatLonCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(includeWavenumberCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(includeIncidenceAngleCheckBox, gbc);

        final JPanel slidersPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = DialogUtils.createGridBagConstraints();
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Pairs selection:"));

        final int sliderValue[] = new int[3];
        final JFreeChart chart = getPairsAndDrawChart(sliderValue, false);
        chartPanel = new ChartPanel(chart);
        gbc2.gridy = 0;
        gbc2.gridx = 2;
        gbc2.gridheight = 9;
        slidersPanel.add(chartPanel, gbc2);

        gbc2.gridy++;
        gbc2.gridx = 0;
        gbc2.gridheight = 1;
        slidersPanel.add(maxDopplerCentroidDiffLabel, gbc2);
        gbc2.gridy++;
        slidersPanel.add(maxDopplerCentroidDiffSlider, gbc2);

        gbc2.gridy++;
        slidersPanel.add(maxSpatialBaselineDiffLabel, gbc2);
        gbc2.gridy++;
        slidersPanel.add(maxSpatialBaselineDiffSlider, gbc2);

        gbc2.gridy++;
        slidersPanel.add(maxTemporalBaselineDiffLabel, gbc2);
        gbc2.gridy++;
        slidersPanel.add(maxTemporalBaselineDiffSlider, gbc2);

        final JPanel radioButtonsPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc3 = DialogUtils.createGridBagConstraints();
        radioButtonsPanel.setBorder(BorderFactory.createTitledBorder("Y-axis for plot:"));

        gbc3.gridy = 0;
        radioButtonsPanel.add(dopplerCentroidRadioButton, gbc3);
        gbc3.gridy++;
        radioButtonsPanel.add(spatialBaselineRadioButton, gbc3);

        gbc2.gridy++;
        gbc2.gridwidth = 1;
        slidersPanel.add(radioButtonsPanel, gbc2);

        gbc2.gridy++;
        slidersPanel.add(savePlotButton, gbc2);

        gbc.gridy++;
        gbc.gridwidth = 2;
        contentPane.add(slidersPanel, gbc);

        return contentPane;
    }

    private JFreeChart getPairsAndDrawChart(final int[] sliderValue, final boolean showLogoInPlot) {

        // Get metadata of acquisitions
        final double[][] metadata = getAcquisitionsMetadata();
        if (metadata != null) {
            // Get arcs corresponding to pairs
            final List<int[]> arcs = getPairs(sliderValue, metadata);

            // Generate series
            final XYSeries acquisitionsSeries = new XYSeries("Acquisitions");
            final XYLineAnnotation[] interferogramArcsAnnotations = new XYLineAnnotation[arcs.size()];

            final String yAxisLabel = yAxisForPlot;
            int metadata_index = yAxisLabel.contains("Doppler") ? 1 : 2;

            for (int i = 0; i < metadata.length; i++) {
                acquisitionsSeries.add(metadata[i][0], metadata[i][metadata_index]);
            }

            for (int i = 0; i < arcs.size(); i++) {
                final int[] arc = arcs.get(i);
                interferogramArcsAnnotations[i] = new XYLineAnnotation(metadata[arc[0]][0], metadata[arc[0]][metadata_index],
                                                                       metadata[arc[1]][0], metadata[arc[1]][metadata_index],
                                                                       new BasicStroke(1.0f),
                                                                       Color.WHITE);
            }

            // Add series to dataset
            final XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(acquisitionsSeries);

            // Generate graph
            final JFreeChart chart = ChartFactory.createXYLineChart(
                    "Reference-Secondaries Plot",
                    "Time [days]",
                    yAxisLabel,
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            chart.addSubtitle(new TextTitle(String.format("(Number of pairs: %d)", pairs.length)));

            final XYPlot plot = (XYPlot) chart.getPlot();
            for (XYLineAnnotation annotation : interferogramArcsAnnotations) {
                plot.addAnnotation(annotation);
            }
            final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

            renderer.setSeriesPaint(0, Color.BLUE);
            renderer.setSeriesLinesVisible(0, false);
            renderer.setSeriesShapesVisible(0, true);

            plot.setBackgroundPaint(Color.gray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRenderer(renderer);

            if (showLogoInPlot) {
                Image image = null;
                final String homePath = SystemUtils.getApplicationHomeDir().getAbsolutePath();
                final String internalPath = "snap-desktop/snap-application/target/snap";
                final String pathUser = homePath.substring(0, homePath.length() - internalPath.length());
                try {
                    image = ImageIO.read(new File(pathUser +
                                                          "s1tbx/s1tbx-rcp/src/main/resources/org/esa/s1tbx/dat/icons/SNAP_icon_128.jpg"));
                } catch (IOException e) {
                }
                plot.setBackgroundImage(image);
                plot.setBackgroundImageAlignment(25);
                plot.setBackgroundAlpha(0.9f);
            }

            return chart;
        }
        return null;
    }

    private List<int[]> getPairs(final int[] sliderValue, final double[][] metadata) {

        final List<int[]> arcs = new ArrayList<>();
        if (metadata != null) {
            // Generate arcs based on constraints set by sliders
            for (int n = 0; n < metadata.length; n++) {
                for (int m = n + 1; m < metadata.length; m++) {
                    final double temporalBaseline = Math.abs(metadata[n][0] - metadata[m][0]);
                    final double dopplerDiff = Math.abs(metadata[n][1] - metadata[m][1]);
                    final double spatialBaseline = Math.abs(metadata[n][2] - metadata[m][2]);

                    if (temporalBaseline <= sliderValue[0]
                            && dopplerDiff <= sliderValue[1]
                            && spatialBaseline <= sliderValue[2]) {
                        arcs.add(new int[]{n, m});
                    }
                }
            }

            // Set pairs
            final String[] dates = getDates();
            if (dates != null) {
                pairs = new String[arcs.size()];
                for (int i = 0; i < pairs.length; i++) {
                    pairs[i] = String.join("-", dates[arcs.get(i)[0]], dates[arcs.get(i)[1]]);
                }
            }
        }

        return arcs;
    }

    private int[][] getMinMaxMean() {

        final double[][] metadata = getAcquisitionsMetadata();
        final int[][] minMaxMean = new int[3][3];
        for (int k = 0; k < 3; k++) {
            minMaxMean[k][0] = Integer.MAX_VALUE;
        }
        if (metadata != null) {
            for (int k = 0; k < 3; k++) {
                for (int n = 0; n < metadata.length; n++) {
                    for (int m = n + 1; m < metadata.length; m++) {
                        final double arc = Math.abs(metadata[n][k] - metadata[m][k]);
                        // Min
                        if ((int) arc < minMaxMean[k][0]) {
                            minMaxMean[k][0] = (int) arc;
                        }
                        // Max
                        if ((int) Math.ceil(arc) > minMaxMean[k][1]) {
                            minMaxMean[k][1] = (int) Math.ceil(arc);
                        }
                    }
                }
            }

            // Mean
            for (int k = 0; k < 3; k++) {
                minMaxMean[k][2] = Math.round(minMaxMean[k][0] + minMaxMean[k][1]) / 2;
            }
        }

        return minMaxMean;
    }

    private double[][] getAcquisitionsMetadata() {

        if (hasSourceProducts()) {
            final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if(abs.containsElement("Baselines")) {
                final MetadataElement baselinesElem = abs.getElement("Baselines");
                final MetadataElement referenceElem = baselinesElem.getElements()[0];

                final MetadataElement[] secondariesElem = referenceElem.getElements();
                final double[][] metadata = new double[secondariesElem.length][3];
                for (int i = 0; i < secondariesElem.length; i++) {
                    metadata[i][0] = -secondariesElem[i].getAttributeDouble("Temp Baseline");
                    metadata[i][1] = secondariesElem[i].getAttributeDouble("Doppler Difference");
                    metadata[i][2] = secondariesElem[i].getAttributeDouble("Perp Baseline");
                }
                return metadata;
            }
        }
        return null;
    }

    private String[] getDates() { // returns array of the form {"ddMMMyyyy", ..., "ddMMMyyy"}

        if (hasSourceProducts()) {
            final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if(abs.containsElement("Baselines")) {
                final MetadataElement baselinesElem = abs.getElement("Baselines");
                final MetadataElement referenceElem = baselinesElem.getElements()[0];

                final String[] secondariesNames = referenceElem.getElementNames();
                for (int i = 0; i < secondariesNames.length; i++) {
                    secondariesNames[i] = secondariesNames[i].substring(7);
                }
                return secondariesNames;
            }
        }
        return null;
    }
}