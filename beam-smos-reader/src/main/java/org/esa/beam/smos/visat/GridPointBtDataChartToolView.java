package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.SmosFormats;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;

public class GridPointBtDataChartToolView extends GridPointBtDataToolView {
    public static final String ID = GridPointBtDataChartToolView.class.getName();

    private JFreeChart chart;
    private YIntervalSeriesCollection dataset;
    private XYPlot plot;
    private JCheckBox[] modeCheckers;
    private static final double INCIDENCE_ANGLE_FACTOR = (90.0 / (1 << 16));
    private static final double NOISE_FACTOR = (50.0 / (1 << 16));

    public GridPointBtDataChartToolView() {
    }

    @Override
    protected JComponent createGridPointComponent() {
        dataset = new YIntervalSeriesCollection();
        chart = ChartFactory.createXYLineChart(null,
                                               null,
                                               null,
                                               dataset,
                                               PlotOrientation.VERTICAL,
                                               true, // Legend?
                                               true,
                                               false);

        DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesFillPaint(0, new Color(255, 127, 127));
        renderer.setSeriesFillPaint(1, new Color(127, 127, 255));

        plot = chart.getXYPlot();
        plot.setNoDataMessage("No data");
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setRenderer(renderer);

        final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setLabel("Incidence Angle (deg)");
        xAxis.setAutoRangeIncludesZero(false);

        final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);

        return new ChartPanel(chart);
    }

    @Override
    protected void updateClientComponent(ProductSceneView smosView) {
        // todo - enable/disable HV modes depending on D1C/F1C
        for (JCheckBox modeChecker : modeCheckers) {
            modeChecker.setEnabled(smosView != null);
        }
    }

    @Override
    protected JComponent createGridPointComponentOptionsComponent() {
        modeCheckers = new JCheckBox[]{
                new JCheckBox("H", true),
                new JCheckBox("V", true),
                new JCheckBox("HV", true),
        };
        final JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        for (JCheckBox modeChecker : modeCheckers) {
            optionsPanel.add(modeChecker);
        }
        return optionsPanel;
    }

    @Override
    protected void updateGridPointBtDataComponent(GridPointBtDataset ds) {
        dataset.removeAllSeries();


        int ix = ds.getColumnIndex("Incidence_Angle");
        int iq = ds.getColumnIndex("Flags");
        int id = ds.getColumnIndex("Pixel_Radiometric_Accuracy");
        if (ix != -1 && iq != -1 && id != -1) {
            int iy1 = ds.getColumnIndex("BT_Value");
            if (iy1 != -1) {

                YIntervalSeries series1 = new YIntervalSeries("BT_H");
                YIntervalSeries series2 = new YIntervalSeries("BT_V");
                boolean m1 = modeCheckers[0].isSelected();
                boolean m2 = modeCheckers[1].isSelected();
                int length = ds.data.length;
                for (int i = 0; i < length; i++) {
                    int polMode = ds.data[i][iq].intValue() & SmosFormats.L1C_POL_FLAGS_MASK;
                    double x = ds.data[i][ix].doubleValue()  * INCIDENCE_ANGLE_FACTOR;
                    double y = ds.data[i][iy1].doubleValue();
                    double dev = ds.data[i][id].doubleValue() * NOISE_FACTOR;
                     if (m1 && polMode == SmosFormats.L1C_POL_MODE_X) {
                        series1.add(x, y, y - dev, y + dev);
                    } else if (m2 && polMode == SmosFormats.L1C_POL_MODE_Y) {
                        series2.add(x, y, y - dev, y + dev);
                    }
                }
                dataset.addSeries(series1);
                dataset.addSeries(series2);
            } else {
                int iy2;
                iy1 = ds.getColumnIndex("BT_Value_Real");
                iy2 = ds.getColumnIndex("BT_Value_Imag");
                if (iy1 != -1 && iy2 != -1) {
                    YIntervalSeries series1 = new YIntervalSeries("BT_H");
                    YIntervalSeries series2 = new YIntervalSeries("BT_V");
                    YIntervalSeries series3 = new YIntervalSeries("BT_HV_Re");
                    YIntervalSeries series4 = new YIntervalSeries("BT_HV_Im");
                    boolean m1 = modeCheckers[0].isSelected();
                    boolean m2 = modeCheckers[1].isSelected();
                    boolean m3 = modeCheckers[2].isSelected();
                    int length = ds.data.length;
                    for (int i = 0; i < length; i++) {
                        int polMode = ds.data[i][iq].intValue() & SmosFormats.L1C_POL_FLAGS_MASK;
                        double dev = ds.data[i][id].doubleValue() * NOISE_FACTOR;
                        double x = ds.data[i][ix].doubleValue() * INCIDENCE_ANGLE_FACTOR;
                        double y1 = ds.data[i][iy1].doubleValue();
                        if (m1 && polMode == SmosFormats.L1C_POL_MODE_X) {
                            series1.add(x, y1, y1 - dev, y1 + dev);
                        } else if (m2 && polMode == SmosFormats.L1C_POL_MODE_Y) {
                            series2.add(x, y1, y1 - dev, y1 + dev);
                        } else if (m3 && (polMode == SmosFormats.L1C_POL_MODE_XY1 || polMode == SmosFormats.L1C_POL_MODE_XY2)) {
                            double y2 = ds.data[i][iy2].doubleValue();
                            series3.add(x, y1, y1 - dev, y1 + dev);
                            series4.add(x, y2, y2 - dev, y2 + dev);
                        }
                    }
                    dataset.addSeries(series1);
                    dataset.addSeries(series2);
                    dataset.addSeries(series3);
                    dataset.addSeries(series4);
                }
            }
        } else {
            plot.setNoDataMessage("Not a SMOS D1C/F1C pixel.");
        }
        chart.fireChartChanged();
    }

    @Override
    protected void updateGridPointBtDataComponent(IOException e) {
        dataset.removeAllSeries();
        plot.setNoDataMessage("I/O error");
    }

    @Override
    protected void clearGridPointBtDataComponent() {
        dataset.removeAllSeries();
        plot.setNoDataMessage("No data");
    }


}
