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

import org.esa.beam.util.ObjectUtils;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

/**
 * The <code>DiagramCanvas</code> class is a UI component used to display simple X/Y plots represented by objects of
 * type <code>{@link Diagram}</code>.
 */
public class DiagramCanvas extends JPanel {

    private Diagram diagram;
    private String messageText;
    private Insets insets;
    private DiagramGraph selectedGraph;
    private Point dragPoint;

    public DiagramCanvas() {
        addComponentListener(new ComponentAdapter() {
            /**
             * Invoked when the component's size changes.
             */
            @Override
            public void componentResized(ComponentEvent e) {
                if (diagram != null) {
                    diagram.invalidate();
                }
            }
        });
        MouseInputAdapter mouseHandler = new IndicatorHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        Diagram oldValue = this.diagram;
        if (oldValue != diagram) {
            this.diagram = diagram;
            repaint();
        }
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        String oldValue = this.messageText;
        if (!ObjectUtils.equalObjects(oldValue, messageText)) {
            this.messageText = messageText;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!(g instanceof Graphics2D)) {
            return;
        }

        final Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        insets = getInsets(insets);
        final int width = getWidth() - (insets.left + insets.right);
        final int height = getHeight() - (insets.top + insets.bottom);
        final int x0 = insets.left;
        final int y0 = insets.top;

        if (diagram != null) {
            diagram.render(g2D, x0, y0, width, height);
        }

        if (messageText != null) {
            drawTextBox(g2D, this.messageText, x0 + width / 2, x0 + height / 2);
        }

        if (dragPoint != null && selectedGraph != null) {
            Diagram.RectTransform transform = diagram.getTransform();
            Point2D a = transform.transformB2A(dragPoint, null);
            double x = a.getX();
            if (x < selectedGraph.getXMin()) {
                x = selectedGraph.getXMin();
            }
            if (x > selectedGraph.getXMax()) {
                x = selectedGraph.getXMax();
            }
            double y = getY(selectedGraph, x);
            Point2D b = transform.transformA2B(new Point2D.Double(x, y), null);

            g2D.setStroke(new BasicStroke(1.0f));
            g2D.setColor(Color.BLACK);
            Ellipse2D.Double marker = new Ellipse2D.Double(b.getX() - 3,
                                                           b.getY() - 3,
                                                           6, 6);
            g2D.draw(marker);
            String text = selectedGraph.getYName() + ": x = " + x + ", y = " + y + ")";
            drawTextBox(g2D, text, 10, 10);
        }
    }

    private void drawTextBox(Graphics2D g2D, String text, int x0, int y0) {
        final FontMetrics fontMetrics = g2D.getFontMetrics();
        final Rectangle2D textBounds = fontMetrics.getStringBounds(text, g2D);
        Rectangle2D.Double r = new Rectangle2D.Double(x0 + textBounds.getX() - 2, 
                                                      y0 + textBounds.getY() - 2,
                                                      textBounds.getWidth() + 4,
                                                      textBounds.getHeight() + 4);
//        if (r.getMaxX() > getWidth()) {
//            r.setRect(getWidth() - r.getWidth(), r.getY(), r.getWidth(), r.getHeight());
//        }
//        if (r.getMinX() < 0) {
//            r.setRect(0, r.getY(), r.getWidth(), r.getHeight());
//        }
//        if (r.getMaxY() > getHeight()) {
//            r.setRect(r.getX(), getHeight() - r.getHeight(), r.getWidth(), r.getHeight());
//        }
//        if (r.getMinY() < 0) {
//            r.setRect(r.getX(), 0, r.getWidth(), r.getHeight());
//        }
        g2D.setColor(new Color(255, 192, 192));
        g2D.fill(r);
        g2D.setColor(Color.black);
        g2D.draw(r);
        g2D.drawString(text, (int) x0, (int) (y0 + fontMetrics.getAscent()));
    }

    public double getY(DiagramGraph graph, double x) {
        int n = graph.getNumValues();
        double x1, y1, x2 = 0, y2 = 0;
        for (int i = 0; i < n; i++) {
            x1 = x2;
            y1 = y2;
            x2 = graph.getXValueAt(i);
            y2 = graph.getYValueAt(i);
            if (i > 0) {
                if (x >= x1 && x <= x2) {
                    return y1 + (x - x1) / (x2 - x1) * (y2 - y1);
                }
            }
        }
        throw new IllegalArgumentException("x out of bounds: " + x);
    }

    private class IndicatorHandler extends MouseInputAdapter {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (getDiagram() == null) {
                return;
            }
            if (selectedGraph == null) {
                selectedGraph = getDiagram().getClosestGraph(e.getX(), e.getY());
            }
            System.out.println("selectedGraph = " + selectedGraph);
            if (selectedGraph != null) {
                dragPoint = e.getPoint();
            } else {
                dragPoint = null;
            }
            repaint();
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            selectedGraph = null;
            dragPoint = null;
            repaint();
        }
    }
}
