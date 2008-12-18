package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.FlagDescriptor;
import org.esa.beam.dataio.smos.SmosFormats;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleInsets;

import javax.swing.JComponent;
import java.awt.Color;
import java.io.IOException;

public class GridPointBtDataFlagmatrixToolView extends GridPointBtDataToolView {

    public static final String ID = GridPointBtDataFlagmatrixToolView.class.getName();
    private static final String SERIES_KEY = "Flags";

    private JFreeChart chart;
    private DefaultXYZDataset dataset;
    private XYPlot plot;

    public GridPointBtDataFlagmatrixToolView() {
    }

    @Override
    protected JComponent createGridPointComponent() {
        dataset = new DefaultXYZDataset();

        NumberAxis xAxis = new NumberAxis("Record #");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);

        // create Method
        String[] flagNames = createFlagNames();
        NumberAxis yAxis = new SymbolAxis(null, flagNames);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
        yAxis.setInverted(true);

        LookupPaintScale paintScale = new LookupPaintScale(0.0, 4.0, Color.WHITE);
        paintScale.add(0.0, Color.BLACK);
        paintScale.add(1.0, Color.RED);
        paintScale.add(2.0, Color.GREEN);
        paintScale.add(3.0, Color.BLUE);
        paintScale.add(4.0, Color.YELLOW);

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setPaintScale(paintScale);
        renderer.setBaseToolTipGenerator(new FlagToolTipGenerator(flagNames));

        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setForegroundAlpha(0.5f);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage("No data");

        chart = new JFreeChart(null, plot);
        chart.removeLegend();

        return new ChartPanel(chart);
    }

    @Override
    protected void updateClientComponent(ProductSceneView smosView) {
    }

    @Override
    protected void updateGridPointBtDataComponent(GridPointBtDataset ds) {
        dataset.removeSeries(SERIES_KEY);

        int iq = ds.getColumnIndex(SmosFormats.BT_FLAGS_NAME);
        if (iq != -1) {
            final int m = ds.data.length;
            final int n = SmosFormats.L1C_FLAGS.length;
            double[][] data = new double[3][n * m];
            for (int x = 0; x < m; x++) {
                final int flags = ds.data[x][iq].intValue();
                for (int y = 0; y < n; y++) {
                    data[0][y * m + x] = (1 + x);
                    data[1][y * m + x] = y;
                    data[2][y * m + x] = ((flags & (1 << y)) != 0) ? (1 + y % 3) : 0.0;
                }
            }
            dataset.addSeries(SERIES_KEY, data);
        } else {
            plot.setNoDataMessage("Not a SMOS D1C/F1C pixel.");
        }
        chart.fireChartChanged();
    }

    @Override
    protected void updateGridPointBtDataComponent(IOException e) {
        dataset.removeSeries(SERIES_KEY);
        plot.setNoDataMessage("I/O error");
    }

    @Override
    protected void clearGridPointBtDataComponent() {
        dataset.removeSeries(SERIES_KEY);
        plot.setNoDataMessage("No data");
    }

    private String[] createFlagNames() {
        final FlagDescriptor[] flags = SmosFormats.L1C_FLAGS;
        String[] flagNames = new String[flags.length];
        for (int i = 0; i < flags.length; i++) {
            flagNames[i] = flags[i].getName();
        }
        return flagNames;
    }


    private class FlagToolTipGenerator implements XYZToolTipGenerator {

        private String[] flagNames;

        private FlagToolTipGenerator(String[] flagNames) {
            this.flagNames = flagNames;
        }

        @Override
        public String generateToolTip(XYDataset xyDataset, int series, int item) {
            return generateToolTip((XYZDataset) xyDataset, series, item);
        }

        @Override
        public String generateToolTip(XYZDataset xyzDataset, int series, int item) {
            int recordIndex = dataset.getX(series, item).intValue();
            int flagIndex = dataset.getY(series, item).intValue();
            boolean flagSet = dataset.getZ(series, item).intValue() != 0;
            String flagName = "?";
            if (flagIndex >= 0 && flagIndex < flagNames.length) {
                flagName = flagNames[flagIndex];
            }
            return flagName + "(" + recordIndex + "): " + (flagSet ? "ON" : "OFF");
        }
    }
}