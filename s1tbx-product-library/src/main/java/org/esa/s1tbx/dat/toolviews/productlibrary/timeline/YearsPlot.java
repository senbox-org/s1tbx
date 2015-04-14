package org.esa.s1tbx.dat.toolviews.productlibrary.timeline;

import org.esa.s1tbx.dat.toolviews.productlibrary.model.DatabaseStatistics;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Show product counts by years
 */
class YearsPlot extends TimelinePlot {

    YearsPlot(final DatabaseStatistics stats) {
        super(stats);
    }

    /**
     * Paints the panel component
     *
     * @param g2d The Graphics
     */
    @Override
    protected void paintPlot(final Graphics2D g2d) {

        final Map<Integer, DatabaseStatistics.YearData> yearDataMap = stats.getYearDataMap();
        final SortedSet<Integer> years = new TreeSet<>(yearDataMap.keySet());
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
            drawButton(g2d, String.valueOf(year), (int)x-20, y, yearDataMap.get(year).isSelected());

            final float newH = (yearDataMap.get(year).yearCnt/(float)maxYearCnt) * h;
            drawBar(g2d, (int)(x-barWidth), h-(int)newH, (int)halfInterval, Math.max(1, (int)newH), h);

            x += interval;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = (int)(e.getX() / interval);

        final Map<Integer, DatabaseStatistics.YearData> yearDataMap = stats.getYearDataMap();
        final SortedSet<Integer> sortedYears = new TreeSet<>(yearDataMap.keySet());
        final Integer[] years = sortedYears.toArray(new Integer[sortedYears.size()]);
        final DatabaseStatistics.YearData yearData = yearDataMap.get(years[x]);

        yearData.setSelected(!yearData.isSelected());

        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int x = (int)(event.getX() / interval);

        final Map<Integer, DatabaseStatistics.YearData> yearDataMap = stats.getYearDataMap();
        final SortedSet<Integer> sortedYears = new TreeSet<>(yearDataMap.keySet());
        final Integer[] years = sortedYears.toArray(new Integer[sortedYears.size()]);

        Integer value = yearDataMap.get(years[x]).yearCnt;
        if(value == 0)
            return "";
        return String.valueOf(years[x])+": "+value;
    }
}
