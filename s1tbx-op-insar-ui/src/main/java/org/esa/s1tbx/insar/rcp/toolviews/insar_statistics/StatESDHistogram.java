/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.toolviews.insar_statistics;

import org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.rcp.SnapApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class StatESDHistogram implements InSARStatistic {

    private JPanel panel;
    private ChartPanel chartPanel;
    private JTextArea textarea;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private Map<String, Map<Integer, Double>> esdData = new HashMap<>();
    private final InSARStatisticsTopComponent parent;

    private static final String TITLE = "Estimated Shifts per Burst Overlap";
    private static final String XAXIS_LABEL = "Burst Overlap #";
    private static final String YAXIS_LABEL = "ESD Measurement [cm]";

    public static final String EmptyMsg = "This tool window requires a coregistered TOPSAR stack product to be selected with ESD applied to it.";

    public StatESDHistogram(final InSARStatisticsTopComponent parent) {
        this.parent = parent;
    }

    public String getName() {
        return "ESD Histogram";
    }

    public Component createPanel() {

        // Add the series to your data set
        dataset = new XYSeriesCollection();

        // Generate the graph
        chart = ChartFactory.createXYLineChart(
                TITLE, // Title
                XAXIS_LABEL, // x-axis Label
                YAXIS_LABEL, // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
        );

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel = new JPanel(new BorderLayout());
        textarea = new JTextArea(EmptyMsg);
        panel.add(textarea, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        setVisible(false);

        return panel;
    }

    public void update(final Product product) {
        try {
            if (InSARStatistic.isValidProduct(product) && readESDMeasure(product)) {
                setVisible(true);

                dataset.removeAllSeries();

                int i = 0;
                for (Map.Entry<String, Map<Integer, Double>> stringMapEntry : esdData.entrySet()) {

                    final XYSeries series = new XYSeries(stringMapEntry.getKey());
                    final Map<Integer, Double> values = stringMapEntry.getValue();
                    for (Map.Entry<Integer, Double> integerDoubleEntry : values.entrySet()) {
                        series.add(integerDoubleEntry.getKey() + 1, integerDoubleEntry.getValue());
                    }

                    dataset.addSeries(series);

                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
                    renderer.setSeriesLinesVisible(i, true);
                    renderer.setSeriesShapesVisible(i, true);

                    chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                    ++i;
                }
            } else {
                setVisible(false);
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    private void setVisible(final boolean flag) {
        textarea.setVisible(!flag);
        chartPanel.setVisible(flag);
    }

    private synchronized boolean readESDMeasure(final Product product) {
        esdData.clear();

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final MetadataElement esdElem = absRoot.getElement(StatESDMeasure.ESD_MEASURE_ELEM);
            if (esdElem != null) {
                final MetadataElement azimuthShiftElem = esdElem.getElement("Azimuth_Shift_Per_Block");
                if (azimuthShiftElem != null) {
                    final MetadataElement[] subSwathElems = azimuthShiftElem.getElements();
                    if (subSwathElems != null) {

                        for (MetadataElement subSwathElem : subSwathElems) {
                            final Map<Integer, Double> shiftMap = new HashMap<>(9);
                            esdData.put(subSwathElem.getName(), shiftMap);

                            final MetadataElement[] overlapElems = subSwathElem.getElements();
                            if (overlapElems != null) {
                                for (MetadataElement overlapElem : overlapElems) {
                                    int overlapIndex = overlapElem.getAttributeInt("overlapIndex");
                                    double azimuthShift = overlapElem.getAttributeDouble("azimuthShift");

                                    shiftMap.put(overlapIndex, azimuthShift);
                                }
                            }
                        }
                    }
                }
            }
        }
        return !esdData.isEmpty();
    }

    public void copyToClipboard() {
        chartPanel.doCopy();
    }

    public void saveToFile() {
        try {
            chartPanel.doSaveAs();
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to save to file", e);
        }
    }
}

