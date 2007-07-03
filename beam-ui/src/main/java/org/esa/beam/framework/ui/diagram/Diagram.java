/*
 * $Id: Diagram.java,v 1.1 2006/10/10 14:47:36 norman Exp $
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

import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.Range;

import java.awt.*;
import java.awt.geom.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>Diagram</code> class is used to plot simple X/Y graphs. Instances of this class are composed of
 * <code>{@link DiagramGraph}</code> and two <code>{@link DiagramAxis}</code> objects for the X and Y axes.
 */
public class Diagram {

    // @todo 2 nf/he - see StatisticsDialog for similar declarations (code smell!)
    private final static String DEFAULT_FONT_NAME = "Verdana";
    private final static int DEFAULT_FONT_SIZE = 9;
    private static final Color DEFAULT_DIAGRAM_BG_COLOR = new Color(200, 200, 255);
    private static final Color DEFAULT_DIAGRAM_FG_COLOR = new Color(0, 0, 100);
    private static final Color DEFAULT_DIAGRAM_TEXT_COLOR = Color.black;

    private List<DiagramGraph> graphs;
    private DiagramAxis xAxis;
    private DiagramAxis yAxis;
    private boolean drawGrid;

    private final AxesPCL axesPCL;

    private Font font;
    private int textGap;
    private int majorTickLength;
    private int minorTickLength;
    private boolean valid;

    // Dependent properties
    //
    private FontMetrics fontMetrics;
    private Rectangle graphArea;
    private String[] yTickTexts;
    private String[] xTickTexts;
    private int maxYTickTextWidth;
    private RectTransform transform;

    public Diagram() {
        graphs = new ArrayList<DiagramGraph>(3);
        axesPCL = new AxesPCL();
        font = new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE);
        textGap = 3;
        majorTickLength = 5;
        minorTickLength = 3;
    }

    public Diagram(DiagramAxis xAxis, DiagramAxis yAxis, DiagramGraph graph) {
        this();
        setXAxis(xAxis);
        setYAxis(yAxis);
        addGraph(graph);
    }

    public RectTransform getTransform() {
        return transform;
    }

    public boolean isDrawGrid() {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    public DiagramAxis getXAxis() {
        return xAxis;
    }

    public void setXAxis(DiagramAxis xAxis) {
        Guardian.assertNotNull("xAxis", xAxis);
        DiagramAxis oldAxis = this.xAxis;
        if (oldAxis != xAxis) {
            if (oldAxis != null) {
                oldAxis.removePropertyChangeListener(axesPCL);
            }
            this.xAxis = xAxis;
            this.xAxis.addPropertyChangeListener(axesPCL);
            invalidate();
        }
    }

    public DiagramAxis getYAxis() {
        return yAxis;
    }

    public void setYAxis(DiagramAxis yAxis) {
        Guardian.assertNotNull("yAxis", yAxis);
        DiagramAxis oldAxis = this.yAxis;
        if (oldAxis != yAxis) {
            if (oldAxis != null) {
                oldAxis.removePropertyChangeListener(axesPCL);
            }
            this.yAxis = yAxis;
            this.yAxis.addPropertyChangeListener(axesPCL);
            invalidate();
        }
    }

    public DiagramGraph[] getGraphs() {
        return graphs.toArray(new DiagramGraph[0]);
    }

    @Deprecated
    public DiagramGraph getGraph() {
        if (graphs.isEmpty()) {
            return null;
        }
        return graphs.get(0);
    }

    @Deprecated
    public void setGraph(DiagramGraph graph) {
        Guardian.assertNotNull("graph", graph);
        graphs.clear();
        graphs.add(graph);
        invalidate();
    }

    public void addGraph(DiagramGraph graph) {
        Guardian.assertNotNull("graph", graph);
        graphs.add(graph);
        invalidate();
    }

    public void removeGraph(DiagramGraph graph) {
        Guardian.assertNotNull("graph", graph);
        graphs.remove(graph);
        invalidate();
    }

    public void removeAllGraphs() {
        graphs.clear();
        invalidate();
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        invalidate();
    }

    public int getTextGap() {
        return textGap;
    }

    public void setTextGap(int textGap) {
        this.textGap = textGap;
        invalidate();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void invalidate() {
        setValid(false);
    }

    public Rectangle getGraphArea() {
        return new Rectangle(graphArea);
    }

    public void render(Graphics2D g2D, int x, int y, int width, int height) {
        Font oldFont = g2D.getFont();
        g2D.setFont(font);
        fontMetrics = g2D.getFontMetrics();

        validate(x, y, width, height);
        drawAxes(g2D, x, y, width, height);
        drawGraphs(g2D);

        g2D.setFont(oldFont);
    }

    private void validate(int x, int y, int width, int height) {
        if (isValid()) {
            return;
        }

        xTickTexts = xAxis.createTickmarkTexts();
        yTickTexts = yAxis.createTickmarkTexts();

        // define y-Axis _values
        final int fontAscent = fontMetrics.getAscent();

        maxYTickTextWidth = 0;
        for (String yTickText : yTickTexts) {
            int sw = fontMetrics.stringWidth(yTickText);
            maxYTickTextWidth = Math.max(maxYTickTextWidth, sw);
        }

        final int widthMaxX = fontMetrics.stringWidth(xTickTexts[xTickTexts.length - 1]);

        int x1 = textGap + fontAscent + textGap + maxYTickTextWidth + textGap + majorTickLength;
        int y1 = textGap + fontAscent / 2;
        int x2 = x + width - (textGap + widthMaxX / 2);
        int y2 = y + height - (textGap + fontAscent + textGap + fontAscent + textGap + majorTickLength);
        final int w = x2 - x1 + 1;
        final int h = y2 - y1 + 1;
        graphArea = new Rectangle(x1, y1, w, h);

        transform = null;
        if (w > 0 && h > 0) {
            transform = new RectTransform(new Range(xAxis.getMinValue(), xAxis.getMaxValue()),
                                   new Range(yAxis.getMinValue(), yAxis.getMaxValue()),
                                   new Range(graphArea.x, graphArea.x + graphArea.width),
                                   new Range(graphArea.y + graphArea.height, graphArea.y));
        }

        setValid(w > 0 && h > 0);
    }

    private void drawGraphs(Graphics2D g2D) {
        if (!isValid()) {
            return;
        }

        final Rectangle clipBounds = g2D.getClipBounds();
        g2D.setClip(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        Point2D.Double a = new Point2D.Double();
        Point2D.Double b1 = new Point2D.Double();
        Point2D.Double b2 = new Point2D.Double();
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            g2D.setStroke(graph.getStyle().getOutlineStroke());
            g2D.setColor(graph.getStyle().getOutlineColor());
            int n = graph.getNumValues();
            for (int i = 0; i < n; i++) {
                double xa = graph.getXValueAt(i);
                double ya = graph.getYValueAt(i);
                a.setLocation(xa, ya);
                b1.setLocation(b2);
                transform.transformA2B(a, b2);
                if (i > 0) {
                    g2D.draw(new Line2D.Double(b1, b2));
                }
            }
            g2D.setStroke(new BasicStroke(0.5f));
            if (graph.getStyle().isShowingPoints()) {
                for (int i = 0; i < n; i++) {
                    double xa = graph.getXValueAt(i);
                    double ya = graph.getYValueAt(i);
                    a.setLocation(xa, ya);
                    transform.transformA2B(a, b1);
                    Rectangle2D.Double r = new Rectangle2D.Double(b1.getX() - 1.5,
                                                                  b1.getY() - 1.5,
                                                                  3.0, 3.0);
                    g2D.setPaint(graph.getStyle().getFillPaint());
                    g2D.fill(r);
                    g2D.setColor(graph.getStyle().getOutlineColor());
                    g2D.draw(r);
                }
            }
        }

        g2D.setClip(clipBounds);
    }

    private void drawAxes(Graphics2D g2D, int xOffset, int yOffset, int width, int height) {
        if (!isValid()) {
            return;
        }

        g2D.setColor(DEFAULT_DIAGRAM_BG_COLOR);
        g2D.fillRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        int tw;
        int x0, y0, x1, x2, y1, y2, xMin, xMax, yMin, yMax, n, n1, n2;
        String text;

        final int th = fontMetrics.getAscent();

        // draw X major tick lines
        xMin = graphArea.x;
        xMax = graphArea.x + graphArea.width;
        yMin = graphArea.y;
        yMax = graphArea.y + graphArea.height;

        y1 = graphArea.y + graphArea.height;
        n1 = xAxis.getNumMajorTicks();
        n2 = xAxis.getNumMinorTicks();
        n = (n1 - 1) * (n2 + 1) + 1;
        for (int i = 0; i < n; i++) {
            g2D.setColor(DEFAULT_DIAGRAM_TEXT_COLOR);
            x0 = xMin + (i * (xMax - xMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                y2 = y1 + majorTickLength;
                text = xTickTexts[i / (n2 + 1)];
                tw = fontMetrics.stringWidth(text);
                g2D.drawString(text, x0 - tw / 2, y2 + textGap + fontMetrics.getAscent());
            } else {
                y2 = y1 + minorTickLength;
            }
            g2D.drawLine(x0, y1, x0, y2);
            g2D.setColor(DEFAULT_DIAGRAM_BG_COLOR.darker());
            g2D.drawLine(x0, y1, x0, yMin);
        }

        // draw Y major tick lines
        x1 = graphArea.x;
        n1 = yAxis.getNumMajorTicks();
        n2 = yAxis.getNumMinorTicks();
        n = (n1 - 1) * (n2 + 1) + 1;
        for (int i = 0; i < n; i++) {
            g2D.setColor(DEFAULT_DIAGRAM_TEXT_COLOR);
            y0 = yMin + (i * (yMax - yMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                x2 = x1 - majorTickLength;
                text = yTickTexts[n1 - 1 - (i / (n2 + 1))];
                tw = fontMetrics.stringWidth(text);
                g2D.drawString(text, x2 - textGap - tw, y0 + th / 2);
            } else {
                x2 = x1 - minorTickLength;
            }
            g2D.drawLine(x1, y0, x2, y0);
            g2D.setColor(DEFAULT_DIAGRAM_BG_COLOR.darker());
            g2D.drawLine(x1, y0, xMax, y0);
        }

        g2D.setColor(Color.black);
        g2D.drawRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        // draw X axis name and unit
        text = xAxis.getName() + " [" + xAxis.getUnit() + "]";
        tw = fontMetrics.stringWidth(text);
        x1 = graphArea.x + graphArea.width / 2 - tw / 2;
        y1 = yOffset + height - textGap;
        g2D.drawString(text, x1, y1);

        // draw Y axis name and unit
        text = yAxis.getName() + " [" + yAxis.getUnit() + "]";
        tw = fontMetrics.stringWidth(text);
        x1 = graphArea.x - majorTickLength - textGap - maxYTickTextWidth - textGap;
        y1 = graphArea.y + graphArea.height / 2 + tw / 2;
        final AffineTransform oldTransform = g2D.getTransform();
        g2D.translate(x1, y1);
        g2D.rotate(-Math.PI / 2);
        g2D.drawString(text, 0, 0);
        g2D.setTransform(oldTransform);
    }

    public DiagramGraph getClosestGraph(final int x, final int y) {
        double minDist = Double.MAX_VALUE;

        Point2D.Double a = new Point2D.Double();
        Point2D.Double b1 = new Point2D.Double();
        Point2D.Double b2 = new Point2D.Double();
        DiagramGraph closestGraph = null;
        for (DiagramGraph graph : getGraphs()) {
            double minDistGraph = Double.MAX_VALUE;
            int n = graph.getNumValues();
            for (int i = 0; i < n; i++) {
                a.setLocation(graph.getXValueAt(i), graph.getYValueAt(i));
                b1.setLocation(b2);
                transform.transformA2B(a, b2);
                if (i > 0) {
                    Line2D.Double segment = new Line2D.Double(b1, b2);
                    double v = segment.ptSegDist(x, y);
                    if (v < minDistGraph) {
                        minDistGraph = v;
                    }
                }
            }
            if (minDistGraph < minDist) {
                minDist = minDistGraph;
                closestGraph = graph;
            }
        }
        return closestGraph;
    }

    private class AxesPCL implements PropertyChangeListener {

        /**
         * This method gets called when a bound property is changed.
         *
         * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            invalidate();
        }
    }

    public static class RectTransform {
        private AffineTransform transformA2B;
        private AffineTransform transformB2A;

        public RectTransform(Range ax, Range ay,
                             Range bx, Range by) {
            double ax1 = ax.getMin();
            double ax2 = ax.getMax();
            double ay1 = ay.getMin();
            double ay2 = ay.getMax();

            double bx1 = bx.getMin();
            double bx2 = bx.getMax();
            double by1 = by.getMin();
            double by2 = by.getMax();

            transformA2B = new AffineTransform();
            transformA2B.translate(bx1 - ax1 * (bx2 - bx1) / (ax2 - ax1),
                                   by1 - ay1 * (by2 - by1) / (ay2 - ay1));
            transformA2B.scale((bx2 - bx1) / (ax2 - ax1),
                               (by2 - by1) / (ay2 - ay1));

            try {
                transformB2A = transformA2B.createInverse();
            } catch (NoninvertibleTransformException e) {
                throw new IllegalArgumentException();
            }
        }

        public Point2D transformA2B(Point2D a, Point2D b) {
            return transformA2B.transform(a, b);
        }

        public Point2D transformB2A(Point2D b, Point2D a) {
            return transformB2A.transform(b, a);
        }
    }

    public void dispose() {
        xAxis = null;
        yAxis = null;
        graphs.clear();
    }
}
