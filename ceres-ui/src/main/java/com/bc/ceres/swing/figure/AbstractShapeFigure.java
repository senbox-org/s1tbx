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

package com.bc.ceres.swing.figure;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.support.VertexHandle;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A figure that is based on a Java AWT shape geometry.
 * <p>
 * Sub-classes have to provide the actual shape (lines or areas) in model coordinates.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractShapeFigure extends AbstractFigure implements ShapeFigure {

    private static final double SELECTION_TOLERANCE = 24.0;
    private Rank rank;

    protected AbstractShapeFigure() {
    }

    /**
     * Constructor.
     *
     * @param rank          The rank, must be either {@link Rank#AREA} or {@link Rank#LINE}.
     * @param normalStyle   The style used for the "normal" state of the figure.
     * @param selectedStyle The style used for the "selected" state of the figure.
     */
    protected AbstractShapeFigure(Rank rank, FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
        Assert.notNull(rank, "rank");
        this.rank = rank;
        setSelectable(true);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public Rank getRank() {
        return rank;
    }

   protected void setRank(Rank rank) {
       this.rank = rank;
   }

    @Override
    public Rectangle2D getBounds() {
        return getShape().getBounds2D();
    }

    @Override
    public void draw(Rendering rendering) {
        Shape shape = getShape();
        if (shape == null) {
            return;
        }

        Viewport vp = rendering.getViewport();
        Rectangle2D vbounds = vp.getViewBounds();
        Rectangle2D mbounds = vp.getViewToModelTransform().createTransformedShape(vbounds).getBounds2D();
        if (!getBounds().intersects(mbounds)) {
            return;
        }

        Graphics2D g = rendering.getGraphics();
        AffineTransform oldTransform = g.getTransform();
        try {
            g.transform(vp.getModelToViewTransform());
            drawShape(rendering);
        } finally {
            g.setTransform(oldTransform);
        }
    }

    /**
     * Draws the {@link #getShape() shape} and other items that are used to graphically
     * represent the figure, for example labels.
     * For convenience, the rendering's drawing context is pre-transformed,
     * so that drawing of the shape can be performed in model coordinates.
     *
     * @param rendering The rendering.
     *
     */
    protected void drawShape(Rendering rendering) {
        final Viewport vp = rendering.getViewport();
        final Graphics2D g = rendering.getGraphics();
        final Shape shape = getShape();
        if (rank == Rank.AREA) {
            Paint fillPaint = getNormalStyle().getFillPaint();
            if (fillPaint != null) {
                g.setPaint(fillPaint);
                g.fill(shape);
            }
        }

        Paint strokePaint = getNormalStyle().getStrokePaint();
        if (strokePaint != null) {
            Stroke normalStroke = getNormalStyle().getStroke(1.0 / vp.getZoomFactor());
            g.setPaint(strokePaint);
            g.setStroke(normalStroke);
            g.draw(shape);
        }

        if (isSelected()) {
            Paint selectedStrokePaint = getSelectedStyle().getStrokePaint();
            if (selectedStrokePaint != null) {
                Stroke selectedStroke = getSelectedStyle().getStroke(1.0 / vp.getZoomFactor());
                g.setStroke(selectedStroke);
                g.setPaint(selectedStrokePaint);
                g.draw(shape);
            }
        }
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        if (getRank() == Rank.AREA) {
            return getShape().contains(point);
        } else {
            try {
                Point2D viewPoint = m2v.transform(point, null);
                double x = viewPoint.getX() - SELECTION_TOLERANCE / 2;
                double y = viewPoint.getY() - SELECTION_TOLERANCE / 2;
                double w = SELECTION_TOLERANCE;
                double h = SELECTION_TOLERANCE;
                //todo check against circle instead of rectangle
                Rectangle2D.Double aDouble = new Rectangle2D.Double(x, y, w, h);
                Rectangle2D rectangle2D = m2v.createInverse().createTransformedShape(aDouble).getBounds2D();
                return getShape().intersects(rectangle2D);
            } catch (NoninvertibleTransformException e) {
                return false;
            }
        }
    }

    @Override
    public void scale(Point2D refPoint, double sx, double sy) {
        final double x0 = refPoint.getX();
        final double y0 = refPoint.getY();
        final PathIterator pathIterator = getShape().getPathIterator(null);
        final Path2D.Double path = new Path2D.Double(pathIterator.getWindingRule());
        final double[] seg = new double[6];
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg);
            if (type == PathIterator.SEG_MOVETO) {
                seg[0] = x0 + (seg[0] - x0) * sx;
                seg[1] = y0 + (seg[1] - y0) * sy;
                path.moveTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_LINETO) {
                seg[0] = x0 + (seg[0] - x0) * sx;
                seg[1] = y0 + (seg[1] - y0) * sy;
                path.lineTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_QUADTO) {
                seg[0] = x0 + (seg[0] - x0) * sx;
                seg[1] = y0 + (seg[1] - y0) * sy;
                seg[2] = x0 + (seg[2] - x0) * sx;
                seg[3] = y0 + (seg[3] - y0) * sy;
                path.quadTo(seg[0], seg[1], seg[2], seg[3]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                seg[0] = x0 + (seg[0] - x0) * sx;
                seg[1] = y0 + (seg[1] - y0) * sy;
                seg[2] = x0 + (seg[2] - x0) * sx;
                seg[3] = y0 + (seg[3] - y0) * sy;
                seg[4] = x0 + (seg[4] - x0) * sx;
                seg[5] = y0 + (seg[5] - y0) * sy;
                path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (type == PathIterator.SEG_CLOSE) {
                path.closePath();
            }
            pathIterator.next();
        }
        setShape(path);
    }

    @Override
    public void move(double dx, double dy) {
        final PathIterator pathIterator = getShape().getPathIterator(null);
        final Path2D.Double path = new Path2D.Double(pathIterator.getWindingRule());
        final double[] seg = new double[6];
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg);
            if (type == PathIterator.SEG_MOVETO) {
                seg[0] += dx;
                seg[1] += dy;
                path.moveTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_LINETO) {
                seg[0] += dx;
                seg[1] += dy;
                path.lineTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_QUADTO) {
                seg[0] += dx;
                seg[1] += dy;
                seg[2] += dx;
                seg[3] += dy;
                path.quadTo(seg[0], seg[1], seg[2], seg[3]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                seg[0] += dx;
                seg[1] += dy;
                seg[2] += dx;
                seg[3] += dy;
                seg[4] += dx;
                seg[5] += dy;
                path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (type == PathIterator.SEG_CLOSE) {
                path.closePath();
            }
            pathIterator.next();
        }
        setShape(path);
    }

    @Override
    public void rotate(Point2D point, double theta) {
        final AffineTransform transform = new AffineTransform();
        transform.rotate(theta, point.getX(), point.getY());
        final PathIterator pathIterator = getShape().getPathIterator(transform);
        final Path2D.Double path = new Path2D.Double(pathIterator.getWindingRule());
        final double[] seg = new double[6];
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg);
            if (type == PathIterator.SEG_MOVETO) {
                path.moveTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_LINETO) {
                path.lineTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_QUADTO) {
                path.quadTo(seg[0], seg[1], seg[2], seg[3]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (type == PathIterator.SEG_CLOSE) {
                path.closePath();
            }
            pathIterator.next();
        }
        setShape(path);
    }

    @Override
    public double[] getSegment(int index) {
        final PathIterator pathIterator = getShape().getPathIterator(null);
        int i = 0;
        while (!pathIterator.isDone()) {
            if (i == index) {
                final double[] seg = new double[6];
                pathIterator.currentSegment(seg);
                return seg;
            }
            pathIterator.next();
            i++;
        }
        return null;
    }

    @Override
    public void setSegment(int index, double[] newSeg) {
        final Path2D.Double path = new Path2D.Double();
        final PathIterator pathIterator = getShape().getPathIterator(null);
        double[] changedSeg = new double[6];
        Arrays.fill(changedSeg, Double.NaN);
        final double[] seg0 = new double[6];
        int i = 0;
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg0);
            double[] seg = seg0;
            if (i == index) {
                changedSeg = seg.clone();
                seg = newSeg;
            }
            if (type == PathIterator.SEG_MOVETO) {
                path.moveTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_LINETO) {
                if (seg[0] == changedSeg[0] && seg[1] == changedSeg[1]) {
                    path.lineTo(newSeg[0], newSeg[1]);
                } else {
                    path.lineTo(seg[0], seg[1]);
                }
            } else if (type == PathIterator.SEG_QUADTO) {
                path.quadTo(seg[0], seg[1], seg[2], seg[3]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (type == PathIterator.SEG_CLOSE) {
                path.closePath();
            }
            pathIterator.next();
            i++;
        }
        setShape(path);
    }

    @Override
    public void addSegment(int index, double[] segment) {
        final Path2D.Double path = new Path2D.Double();
        final PathIterator pathIterator = getShape().getPathIterator(null);
        final double[] seg = new double[6];
        int i = 0;
        boolean moveToSeen = false;
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg);
            if (i == index) {
                if (i == 0) {
                    path.moveTo(segment[0], segment[1]);
                    moveToSeen = true;
                } else {
                    path.lineTo(segment[0], segment[1]);
                }
            }
            if (type == PathIterator.SEG_MOVETO) {
                if (moveToSeen) {
                    path.lineTo(seg[0], seg[1]);
                } else {
                    path.moveTo(seg[0], seg[1]);
                }
                moveToSeen = true;
            } else if (type == PathIterator.SEG_LINETO) {
                path.lineTo(seg[0], seg[1]);
            } else if (type == PathIterator.SEG_QUADTO) {
                path.quadTo(seg[0], seg[1], seg[2], seg[3]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (type == PathIterator.SEG_CLOSE) {
                path.closePath();
            }
            pathIterator.next();
            i++;
        }
        setShape(path);
    }

    @Override
    public void removeSegment(int index) {
        final Path2D.Double path = new Path2D.Double();
        final PathIterator pathIterator = getShape().getPathIterator(null);
        final double[] seg = new double[6];
        int i = 0;
        boolean moveToSeen = false;
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg);
            if (i != index) {
                if (type == PathIterator.SEG_MOVETO) {
                    moveToSeen = true;
                    path.moveTo(seg[0], seg[1]);
                } else if (type == PathIterator.SEG_LINETO) {
                    if (moveToSeen) {
                        path.lineTo(seg[0], seg[1]);
                    } else {
                        path.moveTo(seg[0], seg[1]);
                    }
                } else if (type == PathIterator.SEG_QUADTO) {
                    if (moveToSeen) {
                        path.quadTo(seg[0], seg[1], seg[2], seg[3]);
                    } else {
                        path.moveTo(seg[0], seg[1]);
                    }
                } else if (type == PathIterator.SEG_CUBICTO) {
                    if (moveToSeen) {
                        path.curveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
                    } else {
                        path.moveTo(seg[0], seg[1]);
                    }
                } else if (type == PathIterator.SEG_CLOSE) {
                    path.closePath();
                }
            }
            pathIterator.next();
            i++;
        }
        setShape(path);
    }

    @Override
    public Object createMemento() {
        return getShape();
    }

    @Override
    public void setMemento(Object memento) {
        setShape((Shape) memento);
    }

    @Override
    public int getMaxSelectionStage() {
        return 4;
    }

    @Override
    public Handle[] createHandles(int selectionStage) {
        // No handles at level 1, only high-lighting, see draw() & isSelected()
        if (selectionStage == 2) {
            return createVertexHandles();
        } else if (selectionStage == 3) {
            return createScaleHandles(0.0);
        } else if (selectionStage == 4) {
            Handle[] vertexHandles = createVertexHandles();
            Handle[] scaleHandles = createScaleHandles(8.0);
            ArrayList<Handle> handles = new ArrayList<Handle>(vertexHandles.length + scaleHandles.length);
            handles.addAll(Arrays.asList(vertexHandles));
            handles.addAll(Arrays.asList(scaleHandles));
            return handles.toArray(new Handle[handles.size()]);
        }
        return NO_HANDLES;
    }

    private Handle[] createVertexHandles() {
        FigureStyle handleStyle = getHandleStyle();
        FigureStyle selectedHandleStyle = getSelectedHandleStyle();
        ArrayList<Handle> handleList = new ArrayList<Handle>();
        PathIterator pathIterator = getShape().getPathIterator(null);
        double[] firstSeg = new double[6];
        Arrays.fill(firstSeg, Double.NaN);
        int segmentIndex = 0;
        while (!pathIterator.isDone()) {
            final double[] seg = new double[6];
            final int type = pathIterator.currentSegment(seg);
            final boolean isEqualToFirst = seg[0] == firstSeg[0] && seg[1] == firstSeg[1];
            if (type != PathIterator.SEG_CLOSE && !isEqualToFirst) {
                handleList.add(new VertexHandle(this, segmentIndex, handleStyle, selectedHandleStyle));
            }
            if (segmentIndex == 0) {
                firstSeg = seg;
            }
            pathIterator.next();
            segmentIndex++;
        }
        return handleList.toArray(new Handle[handleList.size()]);
    }
}