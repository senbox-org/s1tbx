package org.esa.s1tbx.dat.toolviews.productlibrary.timeline;

import org.esa.s1tbx.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Shows product counts over time
 */
class TimelinePlot extends JPanel implements MouseListener {

    protected final DatabaseStatistics stats;
    protected float interval;

    TimelinePlot(final DatabaseStatistics stats) {
        this.stats = stats;
        setToolTipText("");
        addMouseListener(this);
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            final Graphics2D g2d = (Graphics2D) g;
            paintPlot(g2d);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    protected void paintPlot(final Graphics2D g2d) {

        final Map<Integer, DatabaseStatistics.YearData> yearDataMap = stats.getYearDataMap();
        final SortedSet<Integer> years = new TreeSet<>(yearDataMap.keySet());
        final int numYears = years.size();
        final int maxDayCnt = stats.getOverallMaxDayCnt();

        final int w = getWidth();
        final int h = getHeight()-15;
        interval = w / (float)numYears;
        final float halfInterval = interval/2f;

        final int y = getHeight() - 2;
        float x = halfInterval;

        for (Integer year : years) {
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(year), x-20, y);

            final DatabaseStatistics.YearData data = yearDataMap.get(year);

            g2d.setColor(Color.BLACK);
            g2d.drawLine((int)(x-halfInterval), h-10, (int)(x-halfInterval), h+5);
            for (int d = 1; d < 366; d++) {
                final float pctX = d/(float)365;
                final float newH = (data.dayOfYearMap.get(d)/(float)maxDayCnt) * h;
                drawBar(g2d, (int)(x-halfInterval+(pctX*interval)), (int)(h-newH), 1, (int)newH, h);
            }

            x += interval;
        }
    }

    static void drawBar(final Graphics2D g2d, final int x, final int y, final int w, final int h, final int maxH) {

        for(int i=0; i < h; ++i) {
            double pct = Math.max(0.4, (i/(double)maxH));
            g2d.setColor(new Color(7, (int)(150*pct), (int)(255*pct)));
            g2d.drawLine(x, y+h-i, x+w, y+h-i);
        }
    }

    static void drawButton(final Graphics2D g2d, final String text, final int x, final int y, boolean selected) {

        final int rw = g2d.getFontMetrics().stringWidth(text) +10;
        final int rh = g2d.getFontMetrics().getHeight()-4;
        final int rx = x - 5;
        final int ry = y-rh+1;

        if(selected) {
            g2d.setColor(Color.lightGray);
            g2d.fillRoundRect(rx, ry, rw, rh, 5, 5);
        } else {
           // g2d.draw3DRect(rx, ry, rw, rh, true);
        }

        g2d.setColor(Color.blue);
        g2d.drawRoundRect(rx, ry, rw, rh, 5, 5);

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x, y);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e) {}

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e) {}

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e) {}
}
