package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

public abstract class AbstractHandle extends AbstractFigure implements Handle {
    private final Figure figure;
    private final FigureStyle style;
    private final FigureStyle selectedStyle;
    private final FigureChangeListener listener;
    private final Point2D.Double location;
    private Shape shape;
    private boolean selected;

    protected AbstractHandle(Figure figure,
                             FigureStyle style,
                             FigureStyle selectedStyle) {
        this.figure = figure;
        this.style = style;
        this.selectedStyle = selectedStyle;

        this.listener = new AbstractFigureChangeListener() {
            @Override
            public void figureChanged(FigureChangeEvent e) {
                updateLocation();
            }
        };
        this.figure.addListener(listener);
        this.location = new Point2D.Double();
    }

    public double getX() {
        return location.x;
    }
    public double getY() {
        return location.y;
    }

    @Override
    public Point2D getLocation() {
        return (Point2D) location.clone();
    }

    public void setLocation(double x, double y) {
        location.setLocation(x, y);
    }

    public abstract void updateLocation();

    public Figure getFigure() {
        return figure;
    }

    public FigureStyle getStyle() {
        return style;
    }

    public Shape getShape() {
        return shape;
    }

    protected void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public Rank getRank() {
        return Rank.POLYGONAL;
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }

    /**
     * The default implementation returns {@code true}.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean contains(Point2D point) {
        return getShape().contains(point);
    }

    @Override
    public void dispose() {
        super.dispose();
        figure.removeListener(listener);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public abstract void move(double dx, double dy);

    @Override
    public final void draw(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform oldTransform = g.getTransform();

        try {
            AffineTransform m2v = vp.getModelToViewTransform();
            Point2D transfLocation = m2v.transform(location, null);
            AffineTransform newTransform = new AffineTransform(oldTransform);
            newTransform.concatenate(AffineTransform.getTranslateInstance(transfLocation.getX(), transfLocation.getY()));
            g.setTransform(newTransform);

            drawHandle(g);

        } finally {
            g.setTransform(oldTransform);
        }
    }

    protected void drawHandle(Graphics2D g) {
        FigureStyle handleStyle = isSelected() ? selectedStyle : style;

        g.setPaint(handleStyle.getFillPaint());
        g.fill(getShape());

        g.setPaint(handleStyle.getStrokePaint());
        g.setStroke(handleStyle.getStroke());
        g.draw(getShape());
    }
}