package org.esa.nest.dat.toolviews.productlibrary.timeline;

import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Shows product counts over time
 */
class TimelinePlot extends JPanel {

    private final DatabaseStatistics stats;
    private float interval;

    TimelinePlot(final DatabaseStatistics stats) {
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
        final int maxDayCnt = stats.getOverallMaxDayCnt();

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
            //float newH = (data.yearCnt/(float)maxYearCnt) * h;
            //YearsPlot.drawBar(g2d, (int)(x-barWidth), (int)(h-newH), (int)halfInterval, Math.max(1, (int)newH));

            g2d.setColor(Color.BLACK);
            g2d.drawLine((int)(x-halfInterval), h-10, (int)(x-halfInterval), h+5);
            for (int d = 1; d < 366; d++) {
                float pctX = d/(float)365;
                float newH = (data.dayOfYearMap.get(d)/(float)maxDayCnt) * h;
                //float newH = pctX * h;
                YearsPlot.drawBar(g2d, (int)(x-halfInterval+(pctX*interval)), (int)(h-newH), 1, (int)newH, h);
            }

            x += interval;
        }
    }
}