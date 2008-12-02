package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.SmosFile;
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
            int iy1 = ds.getColumnIndex("BT_Value");
            if (iy1 != -1) {
                int iq = ds.getColumnIndex("Flags");

                XYSeries series1 = new XYSeries("BT_HH");
                XYSeries series2 = new XYSeries("BT_VV");
                int length = ds.data.length;
                for (int i = 0; i < length; i++) {
                    int polMode = ds.data[i][iq].intValue() & SmosFile.POL_MODE_MASK;
                    if (polMode == SmosFile.POL_MODE_HH) {
                        series1.add(ds.data[i][ix], ds.data[i][iy1]);
                    } else if (polMode == SmosFile.POL_MODE_VV) {
                        series2.add(ds.data[i][ix], ds.data[i][iy1]);
                    }
                }
                dataset.addSeries(series1);
                dataset.addSeries(series2);
            } else {
                int iy2;
                iy1 = ds.getColumnIndex("BT_Value_Real");
                iy2 = ds.getColumnIndex("BT_Value_Imag");
                if (iy1 != -1 && iy2 != -1) {
                    int iq = ds.getColumnIndex("Flags");

                    XYSeries series1 = new XYSeries("BTR_HH");
                    XYSeries series2 = new XYSeries("BTR_VV");
                    XYSeries series3 = new XYSeries("BTR_HVR");
                    XYSeries series4 = new XYSeries("BTI_HVR");
                    XYSeries series5 = new XYSeries("BTI_HVI");
                    XYSeries series6 = new XYSeries("BTI_HVI");
                    int length = ds.data.length;
                    for (int i = 0; i < length; i++) {
                        int polMode = ds.data[i][iq].intValue() & SmosFile.POL_MODE_MASK;
                        if (polMode == SmosFile.POL_MODE_HH) {
                            series1.add(ds.data[i][ix], ds.data[i][iy1]);
                        } else if (polMode == SmosFile.POL_MODE_VV) {
                            series2.add(ds.data[i][ix], ds.data[i][iy1]);
                        } else if (polMode == SmosFile.POL_MODE_HV_REAL) {
                            series3.add(ds.data[i][ix], ds.data[i][iy1]);
                            series4.add(ds.data[i][ix], ds.data[i][iy2]);
                        } else if (polMode == SmosFile.POL_MODE_HV_IMAG) {
                            series5.add(ds.data[i][ix], ds.data[i][iy1]);
                            series6.add(ds.data[i][ix], ds.data[i][iy2]);
                        }
                    }
                    dataset.addSeries(series1);
                    dataset.addSeries(series2);
                    dataset.addSeries(series3);
                    dataset.addSeries(series4);
                    dataset.addSeries(series5);
                    dataset.addSeries(series6);
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
