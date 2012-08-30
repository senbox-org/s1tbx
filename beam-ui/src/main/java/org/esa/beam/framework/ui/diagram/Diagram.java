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

import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.math.Range;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>Diagram</code> class is used to plot simple X/Y graphs. Instances of this class are composed of
 * <code>{@link DiagramGraph}</code> and two <code>{@link DiagramAxis}</code> objects for the X and Y axes.
 */
public class Diagram {

    public final static String DEFAULT_FONT_NAME = "Verdana";
    public final static int DEFAULT_FONT_SIZE = 9;
    public static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    public static final Color DEFAULT_BACKGROUND_COLOR = new Color(210, 210, 255);

    // Main components: graphs + axes
    private List<DiagramGraph> graphs;
    private DiagramAxis xAxis;
    private DiagramAxis yAxis;

    // Visual properties
    private boolean drawGrid;
    private Font font;
    private int textGap;
    private Color textColor;
    private int majorTickLength;
    private int minorTickLength;
    private Color majorGridColor;
    private Color minorGridColor;
    private Color foregroundColor;
    private Color backgroundColor;


    // Change management
    private ArrayList<DiagramChangeListener> changeListeners;
    private int numMergedChangeEvents;

    // Internal properties
    private boolean valid;
    private double xMinAccum;
    private double xMaxAccum;
    private double yMinAccum;
    private double yMaxAccum;

    // Computed internal properties
    private FontMetrics fontMetrics;
    private Rectangle graphArea;
    private String[] yTickTexts;
    private String[] xTickTexts;
    private int maxYTickTextWidth;
    private RectTransform transform;

    public Diagram() {
        graphs = new ArrayList<DiagramGraph>(3);
        font = new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE);
        textGap = 3;
        majorTickLength = 5;
        minorTickLength = 3;
        drawGrid = true;
        foregroundColor = DEFAULT_FOREGROUND_COLOR;
        backgroundColor = DEFAULT_BACKGROUND_COLOR;
        minorGridColor = DEFAULT_BACKGROUND_COLOR.brighter();
        majorGridColor = DEFAULT_BACKGROUND_COLOR.darker();
        textColor = DEFAULT_FOREGROUND_COLOR;
        changeListeners = new ArrayList<DiagramChangeListener>(3);
        disableChangeEventMerging();
        resetMinMaxAccumulators();
    }

    public Diagram(DiagramAxis xAxis, DiagramAxis yAxis, DiagramGraph graph) {
        this();
        setXAxis(xAxis);
        setYAxis(yAxis);
        addGraph(graph);
        resetMinMaxAccumulatorsFromAxes();
    }

    public void enableChangeEventMerging() {
        numMergedChangeEvents = 0;
    }

    public void disableChangeEventMerging() {
        final boolean changeEventsMerged = numMergedChangeEvents > 0;
        numMergedChangeEvents = -1;
        if (changeEventsMerged) {
            invalidate();
        }
    }

    public RectTransform getTransform() {
        return transform;
    }

    public boolean getDrawGrid() {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid) {
        if (this.drawGrid != drawGrid) {
            this.drawGrid = drawGrid;
            invalidate();
        }
    }

    public DiagramAxis getXAxis() {
        return xAxis;
    }

    public void setXAxis(DiagramAxis xAxis) {
        Guardian.assertNotNull("xAxis", xAxis);
        if (this.xAxis != xAxis) {
            if (this.xAxis != null) {
                this.xAxis.setDiagram(null);
            }
            this.xAxis = xAxis;
            this.xAxis.setDiagram(this);
            invalidate();
        }
    }

    public DiagramAxis getYAxis() {
        return yAxis;
    }

    public void setYAxis(DiagramAxis yAxis) {
        Guardian.assertNotNull("yAxis", yAxis);
        if (this.yAxis != yAxis) {
            if (this.yAxis != null) {
                this.yAxis.setDiagram(null);
            }
            this.yAxis = yAxis;
            this.yAxis.setDiagram(this);
            invalidate();
        }
    }

    public DiagramGraph[] getGraphs() {
        return graphs.toArray(new DiagramGraph[0]);
    }

    public int getGraphCount() {
        return graphs.size();
    }

    public DiagramGraph getGraph(int index) {
        return graphs.get(index);
    }

    public void addGraph(DiagramGraph graph) {
        Guardian.assertNotNull("graph", graph);
        if (graphs.add(graph)) {
            graph.setDiagram(this);
            invalidate();
        }
    }

    public void removeGraph(DiagramGraph graph) {
        Guardian.assertNotNull("graph", graph);
        if (graphs.remove(graph)) {
            graph.setDiagram(null);
            invalidate();
        }
    }

    public void removeAllGraphs() {
        if (getGraphCount() > 0) {
            for (DiagramGraph graph : graphs) {
                graph.setDiagram(null);
            }
            graphs.clear();
            invalidate();
        }
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        if (!ObjectUtils.equalObjects(this.font, font)) {
            this.font = font;
            invalidate();
        }
    }

    public Color getMajorGridColor() {
        return majorGridColor;
    }

    public void setMajorGridColor(Color majorGridColor) {
        if (!ObjectUtils.equalObjects(this.majorGridColor, majorGridColor)) {
            this.majorGridColor = majorGridColor;
            invalidate();
        }
    }

    public Color getMinorGridColor() {
        return minorGridColor;
    }

    public void setMinorGridColor(Color minorGridColor) {
        if (!ObjectUtils.equalObjects(this.minorGridColor, minorGridColor)) {
            this.minorGridColor = minorGridColor;
            invalidate();
        }
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        if (!ObjectUtils.equalObjects(this.foregroundColor, foregroundColor)) {
            this.foregroundColor = foregroundColor;
            invalidate();
        }
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        if (!ObjectUtils.equalObjects(this.backgroundColor, backgroundColor)) {
            this.backgroundColor = backgroundColor;
            invalidate();
        }
    }

    public int getTextGap() {
        return textGap;
    }

    public void setTextGap(int textGap) {
        if (!ObjectUtils.equalObjects(this.font, font)) {
            this.textGap = textGap;
            invalidate();
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void invalidate() {
        setValid(false);
        fireDiagramChanged();
    }

    public Rectangle getGraphArea() {
        return new Rectangle(graphArea);
    }

    public void render(Graphics2D g2d, int x, int y, int width, int height) {
        Font oldFont = g2d.getFont();
        g2d.setFont(font);

        if (!isValid()) {
            validate(g2d, x, y, width, height);
        }
        if (isValid()) {
            drawAxes(g2d, x, y, width, height);
            drawGraphs(g2d);
        }

        g2d.setFont(oldFont);
    }

    private void validate(Graphics2D g2d, int x, int y, int width, int height) {
        fontMetrics = g2d.getFontMetrics();

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

    private void drawGraphs(Graphics2D g2d) {
        final Stroke oldStroke = g2d.getStroke();
        final Color oldColor = g2d.getColor();
        final Rectangle oldClip = g2d.getClipBounds();

        g2d.setClip(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        Point2D.Double a = new Point2D.Double();
        Point2D.Double b1 = new Point2D.Double();
        Point2D.Double b2 = new Point2D.Double();
        DiagramGraph[] graphs = getGraphs();
        for (DiagramGraph graph : graphs) {
            g2d.setStroke(graph.getStyle().getOutlineStroke());
            g2d.setColor(graph.getStyle().getOutlineColor());
            int n = graph.getNumValues();
            for (int i = 0; i < n; i++) {
                double xa = graph.getXValueAt(i);
                double ya = graph.getYValueAt(i);
                if (!Double.isNaN(ya)) {
                    a.setLocation(xa, ya);
                    b1.setLocation(b2);
                    transform.transformA2B(a, b2);
                    if (i > 0) {
                        g2d.draw(new Line2D.Double(b1, b2));
                    }
                }
            }
            g2d.setStroke(new BasicStroke(0.5f));
            if (graph.getStyle().isShowingPoints()) {
                for (int i = 0; i < n; i++) {
                    double xa = graph.getXValueAt(i);
                    double ya = graph.getYValueAt(i);
                    if (!Double.isNaN(ya)) {
                        a.setLocation(xa, ya);
                        transform.transformA2B(a, b1);
                        Rectangle2D.Double r = new Rectangle2D.Double(b1.getX() - 1.5,
                                                                      b1.getY() - 1.5,
                                                                      3.0, 3.0);
                        g2d.setPaint(graph.getStyle().getFillPaint());
                        g2d.fill(r);
                        g2d.setColor(graph.getStyle().getOutlineColor());
                        g2d.draw(r);
                    }
                }
            }
        }

        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
        g2d.setClip(oldClip);
    }

    private void drawAxes(Graphics2D g2d, int xOffset, int yOffset, int width, int height) {
        final Stroke oldStroke = g2d.getStroke();
        final Color oldColor = g2d.getColor();

        g2d.setStroke(new BasicStroke(1.0f));

        g2d.setColor(backgroundColor);
        g2d.fillRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

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
            x0 = xMin + (i * (xMax - xMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                y2 = y1 + majorTickLength;
                text = xTickTexts[i / (n2 + 1)];
                tw = fontMetrics.stringWidth(text);
                g2d.setColor(textColor);
                g2d.drawString(text, x0 - tw / 2, y2 + textGap + fontMetrics.getAscent());
                if (drawGrid) {
                    g2d.setColor(majorGridColor);
                    g2d.drawLine(x0, y1, x0, yMin);
                }
            } else {
                y2 = y1 + minorTickLength;
                if (drawGrid) {
                    g2d.setColor(minorGridColor);
                    g2d.drawLine(x0, y1, x0, yMin);
                }
            }
            g2d.setColor(foregroundColor);
            g2d.drawLine(x0, y1, x0, y2);
        }

        // draw Y major tick lines
        x1 = graphArea.x;
        n1 = yAxis.getNumMajorTicks();
        n2 = yAxis.getNumMinorTicks();
        n = (n1 - 1) * (n2 + 1) + 1;
        for (int i = 0; i < n; i++) {
            y0 = yMin + (i * (yMax - yMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                x2 = x1 - majorTickLength;
                text = yTickTexts[n1 - 1 - (i / (n2 + 1))];
                tw = fontMetrics.stringWidth(text);
                g2d.setColor(textColor);
                g2d.drawString(text, x2 - textGap - tw, y0 + th / 2);
                if (drawGrid) {
                    g2d.setColor(majorGridColor);
                    g2d.drawLine(x1, y0, xMax, y0);
                }
            } else {
                x2 = x1 - minorTickLength;
                if (drawGrid) {
                    g2d.setColor(minorGridColor);
                    g2d.drawLine(x1, y0, xMax, y0);
                }
            }
            g2d.setColor(foregroundColor);
            g2d.drawLine(x1, y0, x2, y0);
        }

        g2d.setColor(foregroundColor);
        g2d.drawRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);

        // draw X axis name and unit
        text = getAxisText(xAxis);
        tw = fontMetrics.stringWidth(text);
        x1 = graphArea.x + graphArea.width / 2 - tw / 2;
        y1 = yOffset + height - textGap;
        g2d.setColor(textColor);
        g2d.drawString(text, x1, y1);

        // draw Y axis name and unit
        text = getAxisText(yAxis);
        tw = fontMetrics.stringWidth(text);
        x1 = graphArea.x - majorTickLength - textGap - maxYTickTextWidth - textGap;
        y1 = graphArea.y + graphArea.height / 2 + tw / 2;
        final AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(x1, y1);
        g2d.rotate(-Math.PI / 2);
        g2d.setColor(textColor);
        g2d.drawString(text, 0, 0);
        g2d.setTransform(oldTransform);

        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
    }

    private String getAxisText(DiagramAxis axis) {
        StringBuilder sb = new StringBuilder(37);
        if (axis.getName() != null && axis.getName().length() > 0) {
            sb.append(axis.getName());
        }
        if (axis.getUnit() != null && axis.getUnit().length() > 0) {
            sb.append(" (");
            sb.append(axis.getUnit());
            sb.append(")");
        }
        return sb.toString();
    }

    public DiagramGraph getClosestGraph(int x, int y) {
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

    public void adjustAxes(boolean reset) {
        if (reset) {
            resetMinMaxAccumulators();
        }
        for (DiagramGraph graph : graphs) {
            adjustAxes(graph);
        }
    }

    protected void adjustAxes(DiagramGraph graph) {
        try {
            enableChangeEventMerging();

            final DiagramAxis xAxis = getXAxis();
            xMinAccum = Math.min(xMinAccum, graph.getXMin());
            xMaxAccum = Math.max(xMaxAccum, graph.getXMax());
            boolean xRangeValid = xMaxAccum > xMinAccum;
            if (xRangeValid) {
                xAxis.setValueRange(xMinAccum, xMaxAccum);
                xAxis.setOptimalSubDivision(4, 6, 5);
            }

            final DiagramAxis yAxis = getYAxis();
            yMinAccum = Math.min(yMinAccum, graph.getYMin());
            yMaxAccum = Math.max(yMaxAccum, graph.getYMax());
            boolean yRangeValid = yMaxAccum > yMinAccum;
            if (yRangeValid) {
                yAxis.setValueRange(yMinAccum, yMaxAccum);
                yAxis.setOptimalSubDivision(3, 6, 5);
            }
        } finally {
            disableChangeEventMerging();
        }
    }

    public void resetMinMaxAccumulators() {
        xMinAccum = +Double.MAX_VALUE;
        xMaxAccum = -Double.MAX_VALUE;
        yMinAccum = +Double.MAX_VALUE;
        yMaxAccum = -Double.MAX_VALUE;
    }

    public void resetMinMaxAccumulatorsFromAxes() {
        xMinAccum = getXAxis().getMinValue();
        xMaxAccum = getXAxis().getMaxValue();
        yMinAccum = getYAxis().getMinValue();
        yMaxAccum = getYAxis().getMaxValue();
    }

    private void fireDiagramChanged() {
        if (numMergedChangeEvents == -1) {
            final DiagramChangeListener[] listeners = getChangeListeners();
            for (DiagramChangeListener listener : listeners) {
                listener.diagramChanged(this);
            }
        } else {
            numMergedChangeEvents++;
        }
    }

    public DiagramChangeListener[] getChangeListeners() {
        return changeListeners.toArray(new DiagramChangeListener[0]);
    }

    public void addChangeListener(DiagramChangeListener listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(DiagramChangeListener listener) {
        if (listener != null) {
            changeListeners.remove(listener);
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
        // first disable listening to what will happen next!
        changeListeners.clear();

        // remove main components
        removeAllGraphs();
        if (xAxis != null) {
            xAxis.setDiagram(null);
            xAxis = null;
        }
        if (yAxis != null) {
            yAxis.setDiagram(null);
            yAxis = null;
        }
    }
}
