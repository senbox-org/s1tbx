package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * Shows product counts by months
 */
class MonthsPlot extends JPanel {

    private final DatabaseStatistics stats;
    private float interval;

    private final String[] monthNames = new String[] {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    MonthsPlot(final DatabaseStatistics stats) {
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

        final DatabaseStatistics.MonthData monthData = stats.getMonthData();
        final Set<Integer> months = monthData.getMonthSet();
        final int numMonths = months.size();
        final int maxMonthCnt = monthData.getMaxMonthCnt();

        final int w = getWidth();
        final int h = getHeight()-15;
        interval = w / (float)numMonths;
        final float halfInterval = interval/2;
        final float barWidth = halfInterval/2;

        final int y = getHeight() - 2;

        float x = halfInterval;
        for (Integer month : months) {
            g2d.setColor(Color.BLACK);
            g2d.drawString(monthNames[month], x-10, y);

            Integer value = monthData.get(month);
            float newH = (value/(float)maxMonthCnt) * h;
            YearsPlot.drawBar(g2d, (int)(x-barWidth), h-(int)newH, (int)halfInterval, (int)newH, h);

            x += interval;
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int x = (int)(event.getX() / interval);

        final Set<Integer> monthSet = stats.getMonthData().getMonthSet();
        final Integer[] months = monthSet.toArray(new Integer[monthSet.size()]);

        int value = stats.getMonthData().get(x);
        if(value == 0)
            return "";
        return monthNames[months[x]]+": "+value;
    }
}