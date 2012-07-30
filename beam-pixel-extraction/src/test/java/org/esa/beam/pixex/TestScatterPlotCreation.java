package org.esa.beam.pixex;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JFrame;

public class TestScatterPlotCreation {

    public static void main(String[] args) {
        final XYSeriesCollection dataSet = new XYSeriesCollection();
        XYSeries data = new XYSeries("data");
        data.add(1.0, 2.0);
        data.add(2.0, 3.0);
        data.add(2.5, 2.0);
        data.add(3.25, 1.5);
        dataSet.addSeries(data);
        final JFreeChart plot = ChartFactory.createScatterPlot("My first scatter plot", "x values", "y values",
                                                               dataSet,
                                                               PlotOrientation.VERTICAL, false, false, false);
        ChartPanel chartPanel = new ChartPanel(plot);
        JFrame frame = new JFrame();
        frame.add(chartPanel);
        frame.setSize(600, 300);
        frame.setVisible(true);
    }

}
