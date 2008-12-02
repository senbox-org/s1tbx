package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.SmosFormats;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.ui.RectangleInsets;

import javax.swing.JComponent;
import java.awt.Color;
import java.io.IOException;

public class SmosGridPointFlagmatrixToolView extends SmosGridPointInfoToolView {
    public static final String ID = SmosGridPointFlagmatrixToolView.class.getName();


    private JFreeChart chart;
    private DefaultXYZDataset dataset;
    private XYPlot plot;

    public SmosGridPointFlagmatrixToolView() {
    }


    @Override
    protected JComponent createGridPointComponent() {
        dataset = new DefaultXYZDataset();

        final SmosFormats.FlagDescriptor[] flags = SmosFormats.L1C_FLAGS;
        String[] flagNames = new String[flags.length];
        for (int i = 0; i < flags.length; i++) {
            flagNames[i] = flags[i].getName();
        }

        NumberAxis xAxis = new NumberAxis("Incidence Angle (deg)");
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);

        SymbolAxis yAxis = new SymbolAxis("Flag", flagNames);
        yAxis.setInverted(true);

        LookupPaintScale paintScale = new LookupPaintScale(0.0, 1.0, Color.WHITE);
        paintScale.add(0.0, Color.GRAY);
        paintScale.add(1.0, Color.BLACK);

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setPaintScale(paintScale);

        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage("No data");

        chart = new JFreeChart("Flagmatrix", plot);
        chart.removeLegend();

        return new ChartPanel(chart);
    }

    @Override
    protected void updateSmosComponent(ProductSceneView oldView, ProductSceneView newView) {
    }

    @Override
    protected void updateGridPointComponent(GridPointDataset ds) {
        dataset.removeSeries("Flags");

        int ix = ds.getColumnIndex("Incidence_Angle");
        int iq = ds.getColumnIndex("Flags");
        if (ix != -1 && iq != -1) {
            double[][] data = new double[3][ds.data.length];
            for (int i = 0; i < ds.data.length; i++) {
                final int flags = ds.data[i][iq].intValue();
                for (int j = 0; j < SmosFormats.L1C_FLAGS.length; j++) {
                    data[0][i] = ds.data[i][ix].doubleValue();
                    data[1][i] = j;
                    data[2][i] = ((flags & (1 >> i)) != 0) ? 1.0 : 0.0;
                }
            }
            dataset.addSeries("Flags", data);
        } else {
            plot.setNoDataMessage("Not a SMOS D1C/F1C pixel.");
        }
        chart.fireChartChanged();
    }

    @Override
    protected void updateGridPointComponent(IOException e) {
        dataset.removeSeries("Flags");
        plot.setNoDataMessage("I/O error");
    }

    @Override
    protected void clearGridPointComponent() {
        dataset.removeSeries("Flags");
        plot.setNoDataMessage("No data");
    }


}