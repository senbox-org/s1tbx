package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.VertexHandle;

import java.awt.BasicStroke;
import java.awt.Color;
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

public abstract class AbstractShapeFigure extends AbstractFigure implements ShapeFigure {

    private Rank rank;
    private FigureStyle normalStyle;
    private FigureStyle selectedStyle;

    protected AbstractShapeFigure() {
        this(true, new DefaultFigureStyle());
    }

    protected AbstractShapeFigure(boolean area, FigureStyle normalStyle) {
        this.rank = area ? Rank.AREA : Rank.LINE;
        this.normalStyle = normalStyle;
        this.selectedStyle = DefaultFigureStyle.createLineStyle(new Color(255, 255, 0, 180),
                                                                new BasicStroke(4.0f));
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

    public FigureStyle getNormalStyle() {
        return normalStyle;
    }

    public void setNormalStyle(FigureStyle normalStyle) {
        this.normalStyle = normalStyle;
        fireFigureChanged();
    }

    public FigureStyle getSelectedStyle() {
        return selectedStyle;
    }

    public void setSelectedStyle(FigureStyle selectedStyle) {
        this.selectedStyle = selectedStyle;
        fireFigureChanged();
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

        } finally {
            g.setTransform(oldTransform);
        }
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        if (getRank() == Rank.AREA) {
            return getShape().contains(point);
        } else {
            try {
                Point2D viewPoint = m2v.transform(point, null);
                Rectangle2D.Double aDouble = new Rectangle2D.Double(viewPoint.getX() - 1.5, viewPoint.getY() - 1.5, 3.0,
                                                                    3.0);
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
                if(seg[0] == changedSeg[0] && seg[1] == changedSeg[1]){
                    path.lineTo(newSeg[0], newSeg[1]);
                }else {
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
        if (selectionStage == 1) {
            // No handles at level 1, only high-lighting, see draw() & isSelected()
            return new Handle[0];
        } else if (selectionStage == 2) {
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
        return new Handle[0];
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