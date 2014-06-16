package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import java.awt.*;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Shows product counts over time
 */
class TimelinePanel extends WebPanel {

    private final DatabaseStatistics stats;

    TimelinePanel(final DatabaseStatistics stats) {
        this.stats = stats;
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        final Map<Integer, DatabaseStatistics.YearData> yearData = stats.getYearData();
        final SortedSet<Integer> years = new TreeSet<>(yearData.keySet());
        final int numYears = years.size();

        final int w = getWidth();
        final float yearInterval = w / numYears;
        final float pct = 1 / (float) (numYears * 365);
        final float dayInterval = w * pct;
        final int yH = getHeight() - 10;

        int yX = 0;
        for (Integer year : years) {
            g2d.drawString(String.valueOf(year), (float) yX, yH);
            yX += yearInterval;

            int dX = 0;
            final DatabaseStatistics.YearData data = yearData.get(year);
            for (int d = 1; d < 366; d++) {
                g2d.fillRect(dX, yH, (int) (dX + dayInterval), data.dayOfYearMap.get(d));
                dX += dayInterval;
            }
        }

        //g.setColor(Color.yellow);
        //g.fill3DRect(0, 0, getWidth(), getHeight(), true);
    }
}