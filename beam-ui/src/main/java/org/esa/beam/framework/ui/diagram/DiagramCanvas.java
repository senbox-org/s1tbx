/*
 * $Id: DiagramCanvas.java,v 1.1 2006/10/10 14:47:36 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.diagram;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import org.esa.beam.util.ObjectUtils;

/**
 * The <code>DiagramCanvas</code> class is a UI component used to display simple X/Y plots represented by objects of
 * type <code>{@link Diagram}</code>.
 */
public class DiagramCanvas extends JPanel {

    private Diagram _diagram;
    private String _messageText;
    private Insets _insets;

    public DiagramCanvas() {
        addComponentListener(new ComponentAdapter() {
            /**
             * Invoked when the component's size changes.
             */
            public void componentResized(ComponentEvent e) {
                if (_diagram != null) {
                    _diagram.invalidate();
                }
            }
        });
    }

    /**
     * Creates a new <code>JPanel</code> with a double buffer and a flow layout.
     */
    public DiagramCanvas(Diagram diagram) {
        this();
        _diagram = diagram;
    }

    public Diagram getDiagram() {
        return _diagram;
    }

    public void setDiagram(Diagram diagram) {
        Diagram oldValue = _diagram;
        if (oldValue != diagram) {
            _diagram = diagram;
            repaint();
        }
    }

    public String getMessageText() {
        return _messageText;
    }

    public void setMessageText(String messageText) {
        String oldValue = _messageText;
        if (!ObjectUtils.equalObjects(oldValue, messageText)) {
            _messageText = messageText;
            repaint();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!(g instanceof Graphics2D)) {
            return;
        }

        final Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        _insets = getInsets(_insets);
        final int width = getWidth() - (_insets.left + _insets.right);
        final int height = getHeight() - (_insets.top + _insets.bottom);
        final int x0 = _insets.left;
        final int y0 = _insets.top;

        if (_diagram != null) {
            _diagram.draw(g2D, x0, y0, width, height);
        }

        if (_messageText != null) {
            final FontMetrics fontMetrics = g2D.getFontMetrics();
            final Rectangle2D stringBounds = fontMetrics.getStringBounds(_messageText, g2D);
            double x = x0 + stringBounds.getX() + (width - stringBounds.getWidth()) / 2;
            double y = y0 + stringBounds.getY() + (height - stringBounds.getHeight()) / 2;
            g2D.setColor(new Color(255, 127, 127));
            g2D.fillRect((int) x - 2, (int) y - 2,
                         (int) stringBounds.getWidth() + 4,
                         (int) stringBounds.getHeight() + 4);
            g2D.setColor(Color.black);
            g2D.drawRect((int) x - 2, (int) y - 2,
                         (int) stringBounds.getWidth() + 4,
                         (int) stringBounds.getHeight() + 4);
            g2D.drawString(_messageText, (int) x, (int) (y + fontMetrics.getAscent()));
        }
    }
}
