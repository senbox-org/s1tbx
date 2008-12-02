package org.esa.beam.smos.visat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.JComponent;
import java.io.IOException;

public class SmosGridPointChartToolView extends SmosGridPointInfoToolView {
    public static final String ID = SmosGridPointChartToolView.class.getName();


    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private XYPlot plot;

    public SmosGridPointChartToolView() {
    }

    @Override
    protected JComponent createGridPointComponent() {
        dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart(null,
                                               null,
                                               null,
                                               dataset,
                                               PlotOrientation.VERTICAL,
                                               true, // Legend?
                                               true,
                                               false);

        plot = chart.getXYPlot();
        plot.setNoDataMessage("No data");
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        return new ChartPanel(chart);
    }

    @Override
    protected void updateGridPointComponent(GridPointDataset ds) {
        dataset.removeAllSeries();

        int ix = ds.getColumnIndex("Incidence_Angle");
        if (ix != -1) {
            int iy = ds.getColumnIndex("BT_Value");
            if (iy != -1) {
                XYSeries series = new XYSeries("BT_Value");
                int length = ds.data.length;
                for (int i = 0; i < length; i++) {
                    series.add(ds.data[i][ix], ds.data[i][iy]);
                }
                dataset.addSeries(series);
            } else {
                int iy1 = ds.getColumnIndex("BT_Value_Real");
                int iy2 = ds.getColumnIndex("BT_Value_Imag");
                if (iy1 != -1 && iy2 != -1) {
                    XYSeries series1 = new XYSeries("BT_Value_Real");
                    XYSeries series2 = new XYSeries("BT_Value_Imag");
                    int length = ds.data.length;
                    for (int i = 0; i < length; i++) {
                        series1.add(ds.data[i][ix], ds.data[i][iy1]);
                        series2.add(ds.data[i][ix], ds.data[i][iy2]);
                    }
                    dataset.addSeries(series1);
                    dataset.addSeries(series2);
                }
            }
        } else {
            plot.setNoDataMessage("Not a SMOS D1C/F1C pixel.");
        }
        chart.fireChartChanged();
    }

    @Override
    protected void updateGridPointComponent(IOException e) {
        dataset.removeAllSeries();
        plot.setNoDataMessage("I/O error");
    }

    @Override
    protected void clearGridPointComponent() {
        dataset.removeAllSeries();
        plot.setNoDataMessage("No data");
    }


}
