package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Show product counts by years
 */
class YearsPanel extends WebPanel {

    private final DatabaseStatistics stats;

    YearsPanel(final DatabaseStatistics stats) {
        this.stats = stats;
        setUndecorated(false);
        add(new JLabel("Years"));
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        final Map<Integer, Integer> yearMap = stats.getYearStats();
        final SortedSet<Integer> years = new TreeSet<>(yearMap.keySet());
        final int numYears = years.size();

        g.setColor(Color.red);
        g.fill3DRect(0, 0, getWidth(), getHeight(), true);
    }
}
