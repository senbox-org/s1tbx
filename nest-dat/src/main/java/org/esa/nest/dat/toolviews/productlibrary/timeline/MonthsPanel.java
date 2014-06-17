package org.esa.nest.dat.toolviews.productlibrary.timeline;

import com.alee.laf.panel.WebPanel;
import org.esa.nest.dat.toolviews.productlibrary.model.DatabaseStatistics;

import javax.swing.*;
import java.awt.*;

/**
 * Shows product counts by months
 */
class MonthsPanel extends WebPanel {

    private final DatabaseStatistics stats;

    MonthsPanel(final DatabaseStatistics stats) {
        this.stats = stats;
        add(new JLabel("Months"));
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

        g.setColor(Color.green);
        g.fill3DRect(0, 0, getWidth(), getHeight(), true);
    }
}