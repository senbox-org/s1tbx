/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.ObjectUtils;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;

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
    private DiagramChangeHandler diagramChangeHandler;

    public DiagramCanvas() {
        setName("diagram");
        diagramChangeHandler = new DiagramChangeHandler();
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
        setPreferredSize(new Dimension(320, 200));
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        Diagram oldDiagram = this.diagram;
        if (oldDiagram != diagram) {
            if (oldDiagram != null) {
                oldDiagram.removeChangeListener(diagramChangeHandler);
            }
            this.diagram = diagram;
            if (this.diagram != null) {
                diagram.addChangeListener(diagramChangeHandler);
            }
            firePropertyChange("diagram", oldDiagram, diagram);
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

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        insets = getInsets(insets);
        final int width = getWidth() - (insets.left + insets.right);
        final int height = getHeight() - (insets.top + insets.bottom);
        final int x0 = insets.left;
        final int y0 = insets.top;

        if (diagram != null) {
            diagram.render(g2d, x0, y0, width, height);
            if (dragPoint != null && selectedGraph != null) {
                drawValueIndicator(g2d);
            }
        }

        if (messageText != null) {
            drawTextBox(g2d, this.messageText, x0 + width / 2, y0 + height / 2, new Color(255, 192, 102));
        }
    }

    private void drawValueIndicator(Graphics2D g2d) {
        Diagram.RectTransform transform = diagram.getTransform();
        Point2D a = transform.transformB2A(dragPoint, null);
        double x = a.getX();
        if (x < selectedGraph.getXMin()) {
            x = selectedGraph.getXMin();
        }
        if (x > selectedGraph.getXMax()) {
            x = selectedGraph.getXMax();
        }
        final Stroke oldStroke = g2d.getStroke();
        final Color oldColor = g2d.getColor();

        double y = getY(selectedGraph, x);
        Point2D b = transform.transformA2B(new Point2D.Double(x, y), null);

        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(diagram.getForegroundColor());
        Ellipse2D.Double marker = new Ellipse2D.Double(b.getX() - 4.0, b.getY() - 4.0, 8.0, 8.0);
        g2d.draw(marker);

        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6, 6}, 12));
        g2d.setColor(diagram.getForegroundColor());
        final Rectangle graphArea = diagram.getGraphArea();
        g2d.draw(new Line2D.Double( b.getX(), graphArea.y + graphArea.height, b.getX(), b.getY()));
        g2d.draw(new Line2D.Double( graphArea.x, b.getY(), b.getX(), b.getY()));

        DecimalFormat decimalFormat = new DecimalFormat("0.#####E0");
        String text = selectedGraph.getYName() + ": x = " + decimalFormat.format(x) + ", y = " + decimalFormat.format(y);

        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);

        drawTextBox(g2d, text, graphArea.x + 6, graphArea.y + 6 + 16, new Color(255, 255, 255, 128));
    }

    private void drawTextBox(Graphics2D g2D, String text, int x0, int y0, Color color) {

        final FontMetrics fontMetrics = g2D.getFontMetrics();
        final Rectangle2D textBounds = fontMetrics.getStringBounds(text, g2D);
        Rectangle2D.Double r = new Rectangle2D.Double(x0 + textBounds.getX() - 2.0,
                                                      y0 + textBounds.getY() - 2.0,
                                                      textBounds.getWidth() + 4.0,
                                                      textBounds.getHeight() + 4.0);

        if (r.getMaxX() > getWidth()) {
            r.setRect(getWidth() - r.getWidth(), r.getY(), r.getWidth(), r.getHeight());
        }
        if (r.getMinX() < 0) {
            r.setRect(0, r.getY(), r.getWidth(), r.getHeight());
        }
        if (r.getMaxY() > getHeight()) {
            r.setRect(r.getX(), getHeight() - r.getHeight(), r.getWidth(), r.getHeight());
        }
        if (r.getMinY() < 0) {
            r.setRect(r.getX(), 0, r.getWidth(), r.getHeight());
        }
        g2D.setColor(color);
        g2D.fill(r);
        g2D.setColor(Color.black);
        g2D.draw(r);
        g2D.drawString(text, x0, y0);
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

    private class DiagramChangeHandler implements DiagramChangeListener {
        public void diagramChanged(Diagram diagram) {
            repaint();
        }
    }
}
