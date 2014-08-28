package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Show product counts by years
 */
class YearsPlot extends JPanel {

    private final DatabaseStatistics stats;
    private float interval;

    YearsPlot(final DatabaseStatistics stats) {
        this.stats = stats;
        setToolTipText("");
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2d = (Graphics2D) g;

        final Map<Integer, DatabaseStatistics.YearData> yearData = stats.getYearData();
        final SortedSet<Integer> years = new TreeSet<>(yearData.keySet());
        final int numYears = years.size();
        final int maxYearCnt = stats.getOverallMaxYearCnt();

        final int w = getWidth();
        final int h = getHeight()-15;
        interval = w / (float)numYears;
        final float halfInterval = interval/2f;
        final float barWidth = halfInterval/2f;

        final int y = getHeight() - 2;
        float x = halfInterval;

        for (Integer year : years) {
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(year), x-20, y);

            final DatabaseStatistics.YearData data = yearData.get(year);
            float newH = (data.yearCnt/(float)maxYearCnt) * h;
            drawBar(g2d, (int)(x-barWidth), h-(int)newH, (int)halfInterval, Math.max(1, (int)newH), h);

            x += interval;
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int x = (int)(event.getX() / interval);

        final Map<Integer, DatabaseStatistics.YearData> yearData = stats.getYearData();
        final SortedSet<Integer> sortedYears = new TreeSet<>(yearData.keySet());
        final Integer[] years = sortedYears.toArray(new Integer[sortedYears.size()]);

        Integer value = yearData.get(years[x]).yearCnt;
        if(value == 0)
            return "";
        return String.valueOf(years[x])+": "+value;
    }

    public static void drawBar(final Graphics2D g2d, int x, int y, int w, int h, int maxH) {

        for(int i=0; i < h; ++i) {
            double pct = Math.max(0.4, (i/(double)maxH));
            g2d.setColor(new Color(7, (int)(150*pct), (int)(255*pct)));
            g2d.drawLine(x, y+h-i, x+w, y+h-i);
        }
    }
}
