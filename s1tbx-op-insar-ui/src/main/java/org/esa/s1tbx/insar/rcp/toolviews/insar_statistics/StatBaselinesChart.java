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

import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent;
import org.esa.snap.core.datamodel.Product;
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
import java.util.List;


public class StatBaselinesChart implements InSARStatistic {

    private JPanel panel;
    private ChartPanel chartPanel;
    private JTextArea textarea;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private final InSARStatisticsTopComponent parent;

    private static final String TITLE = "Baselines";
    private static final String XAXIS_LABEL = "Temporal Baseline";
    private static final String YAXIS_LABEL = "Perpendicular Baseline";

    public static final String EmptyMsg = "This tool window requires a coregistered stack product.";

    public StatBaselinesChart(final InSARStatisticsTopComponent parent) {
        this.parent = parent;
    }

    public String getName() {
        return "Baseline Chart";
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

    private StatBaselines getStatBaselineComponent() {
        List<InSARStatistic> components = parent.getStatisticComponents();
        for (InSARStatistic component : components) {
            if (component instanceof StatBaselines) {
                return (StatBaselines) component;
            }
        }
        return null;
    }

    private StatBaselines.CachedBaseline[] getBaselines(final Product product) {
        final StatBaselines statBaselines = getStatBaselineComponent();
        if (statBaselines != null) {
            return statBaselines.getBaselines(product);
        }
        return null;
    }

    public void update(final Product product) {
        try {
            if (!InSARStatistic.isValidProduct(product)) {
                setVisible(false);
                return;
            }

            final StatBaselines.CachedBaseline[] baselines = getBaselines(product);
            if (baselines == null) {
                setVisible(false);
                return;
            }

            final XYSeries series = new XYSeries("data");
            for (StatBaselines.CachedBaseline baseline : baselines) {
                final InSARStackOverview.IfgPair slave = baseline.getIfgPair();
                if(slave.getSlaveMetadata() != slave.getMasterMetadata()) {
                    series.add(slave.getTemporalBaseline(), slave.getPerpendicularBaseline());
                }
            }

            setVisible(true);
            dataset.removeAllSeries();

            dataset.addSeries(series);

            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, true);

            chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    private void setVisible(final boolean flag) {
        textarea.setVisible(!flag);
        chartPanel.setVisible(flag);
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

