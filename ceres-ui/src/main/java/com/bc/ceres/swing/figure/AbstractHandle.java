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
    private final Point2D location;
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
                setHandleShape();
            }
        };
        this.figure.addListener(listener);
        this.location = new Point2D.Double();
    }

    @Override
    public Point2D getLocation() {
        return (Point2D) location.clone();
    }

    public void setLocation(double x, double y) {
        location.setLocation(x, y);
    }

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

    protected void setHandleShape() {
        setShape(createHandleShape());
    }

    @Override
    public Rank getRank() {
        return Rank.POLYGONAL;
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    protected abstract Shape createHandleShape();

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
    public void draw(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform transformSave = g.getTransform();
        try {
            AffineTransform m2v = vp.getModelToViewTransform();
            Point2D locationView = m2v.transform(location, null);
            g.setTransform(AffineTransform.getTranslateInstance(-locationView.getX(), -locationView.getY()));

            FigureStyle handleStyle = isSelected() ? selectedStyle : style;
            g.setPaint(handleStyle.getFillPaint());
            g.fill(getShape());
            g.setPaint(handleStyle.getDrawPaint());
            g.setStroke(handleStyle.getDrawStroke());
            g.draw(getShape());

        } finally {
            g.setTransform(transformSave);
        }

    }
}