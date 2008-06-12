package org.esa.beam.visat.toolviews.diag;

import com.sun.media.jai.util.CacheDiagnostics;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.UnitType;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.swing.*;
import java.awt.*;


public class JaiMonitor {

    /**
     * The datasets.
     */
    private TimeSeriesCollection[] datasets;

    /**
     * Creates a new monitor panel.
     *
     * @return the monitor panel
     */
    public JPanel createPanel() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(
                new DateAxis("Time")
        );
        this.datasets = new TimeSeriesCollection[4];
        this.datasets[0] = addSubPlot(plot, "#Tiles");
        this.datasets[1] = addSubPlot(plot, "#Hits");
        this.datasets[2] = addSubPlot(plot, "#Misses");
        this.datasets[3] = addSubPlot(plot, "Memory");

//        JFreeChart chart = new JFreeChart("Tile cache usage", plot);
        JFreeChart chart = new JFreeChart(plot);
        LegendTitle legend = (LegendTitle) chart.getSubtitle(0);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setMargin(
                new RectangleInsets(UnitType.ABSOLUTE, 0, 4, 0, 4)
        );
        chart.setBorderPaint(Color.black);
        chart.setBorderVisible(true);
        chart.setBackgroundPaint(Color.white);

        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));
        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds

        ChartPanel chartPanel = new ChartPanel(chart);
        mainPanel.add(chartPanel);

        chartPanel.setPreferredSize(new java.awt.Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return mainPanel;
    }

    public synchronized void updateState() {
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        if (tileCache instanceof CacheDiagnostics) {
            CacheDiagnostics cacheDiagnostics = (CacheDiagnostics) tileCache;
            cacheDiagnostics.enableDiagnostics();
            final Millisecond t = new Millisecond();
            update(0, t, cacheDiagnostics.getCacheTileCount());
            update(1, t, cacheDiagnostics.getCacheHitCount());
            update(2, t, cacheDiagnostics.getCacheMissCount());
            update(3, t, cacheDiagnostics.getCacheMemoryUsed());
        } else {
            // todo - ?
        }
    }

    private static TimeSeriesCollection addSubPlot(CombinedDomainXYPlot plot, String label) {
        final TimeSeriesCollection seriesCollection = new TimeSeriesCollection(new TimeSeries(
                label, Millisecond.class
        ));
        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        XYPlot subplot = new XYPlot(
                seriesCollection, null, rangeAxis,
                new StandardXYItemRenderer()
        );
        subplot.setBackgroundPaint(Color.lightGray);
        subplot.setDomainGridlinePaint(Color.white);
        subplot.setRangeGridlinePaint(Color.white);
        plot.add(subplot);
        return seriesCollection;
    }

    private void update(int i, Millisecond t, double value) {
        this.datasets[i].getSeries(0).add(t, value);
    }
}

