package com.bc.ceres.figure.support;

import com.bc.ceres.figure.AbstractFigure;
import com.bc.ceres.figure.support.FigureStyle;
import com.bc.ceres.figure.Handle;
import com.bc.ceres.figure.support.VertexHandle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;


public class DefaultShapeFigure extends AbstractFigure {
    private Shape shape;
    private Rank rank;
    private FigureStyle style;
    private boolean selected;

    public DefaultShapeFigure(Shape shape, boolean polygonal, FigureStyle style) {
        this.shape = shape;
        this.rank = polygonal ? Rank.POLYGONAL : Rank.LINEAL;
        this.style = style;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape path) {
        shape = path;
        fireFigureChanged();
    }

    public FigureStyle getStyle() {
        return style;
    }

    public void setStyle(FigureStyle style) {
        this.style = style;
        fireFigureChanged();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            fireFigureChanged();
        }
    }

    @Override
    public Rank getRank() {
        return rank;
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (rank == Rank.POLYGONAL) {
            g2d.setPaint(getStyle().getFillPaint());
            g2d.fill(getShape());
        }
        g2d.setPaint(getStyle().getDrawPaint());
        g2d.setStroke(getStyle().getDrawStroke());
        g2d.draw(getShape());
        if (isSelected()) {
            Stroke plainStroke = getStyle().getDrawStroke();
            Stroke selectedStroke;
            if (plainStroke instanceof BasicStroke) {
                BasicStroke basicStroke = (BasicStroke) plainStroke;
                selectedStroke = new BasicStroke(basicStroke.getLineWidth() + 4.0f);
            } else {
                selectedStroke = new BasicStroke(4.0f);
            }
            g2d.setPaint(new Color(255, 255, 0, 150));
            g2d.setStroke(selectedStroke);
            g2d.draw(getShape());
        }
    }

    @Override
    public boolean contains(Point2D point) {
        return getShape().contains(point);
    }

    @Override
    public void scale(Point2D refPoint, double sx, double sy) {
        final double x0 = refPoint.getX();
        final double y0 = refPoint.getY();
        final Path2D.Double path = new Path2D.Double();
        final PathIterator pathIterator = shape.getPathIterator(null);
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
        final PathIterator pathIterator = shape.getPathIterator(null);
        final Path2D.Double path = new Path2D.Double();
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
        final PathIterator pathIterator = shape.getPathIterator(transform);
        final Path2D.Double path = new Path2D.Double();
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
    public double[] getVertex(int index) {
        final PathIterator pathIterator = shape.getPathIterator(null);
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
    public void setVertex(int index, double[] newSeg) {
        final Path2D.Double path = new Path2D.Double();
        final PathIterator pathIterator = shape.getPathIterator(null);
        final double[] seg0 = new double[6];
        int i = 0;
        while (!pathIterator.isDone()) {
            final int type = pathIterator.currentSegment(seg0);
            double[] seg = seg0;
            if (i == index) {
                seg = newSeg;
            }
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
    public int getMaxSelectionLevel() {
        return 4;
    }

    @Override
    public Handle[] createHandles(int selectionLevel) {
        if (selectionLevel == 1) {
            // No handles at level 1, only high-lighting, see draw() & isSelected()
            return new Handle[0];
        } else if (selectionLevel == 2) {
            return createVertexHandles();
        } else if (selectionLevel == 3) {
            return createScaleHandles(0.0);
        } else if (selectionLevel == 4) {
            Handle[] vertexHandles = createVertexHandles();
            Handle[] scaleHandles = createScaleHandles(8.0);
            ArrayList<Handle> handles = new ArrayList<Handle>(vertexHandles.length + scaleHandles.length);
            handles.addAll(Arrays.asList(vertexHandles));
            handles.addAll(Arrays.asList(vertexHandles));
            return handles.toArray(new Handle[handles.size()]);
        }
        return new Handle[0];
    }

    private Handle[] createVertexHandles() {
        FigureStyle handleStyle = getHandleStyle();
        FigureStyle selectedHandleStyle = getSelectedHandleStyle();
        ArrayList<Handle> handleList = new ArrayList<Handle>();
        PathIterator pathIterator = shape.getPathIterator(null);
        int vertexIndex = 0;
        while (!pathIterator.isDone()) {
            final double[] seg = new double[6];
            final int type = pathIterator.currentSegment(seg);
            if (type != PathIterator.SEG_CLOSE) {
                handleList.add(new VertexHandle(this, vertexIndex, handleStyle, selectedHandleStyle));
            }
            pathIterator.next();
            vertexIndex++;
        }
        return handleList.toArray(new Handle[handleList.size()]);
    }

    @Override
    public DefaultShapeFigure clone() {
        DefaultShapeFigure f = (DefaultShapeFigure) super.clone();
        f.shape = new Path2D.Double(shape);
        f.style = style.clone();
        return f;
    }
}
