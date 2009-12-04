package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class AbstractPointFigure extends AbstractFigure {
    private final Point2D.Double location;
    private final double radius;

    protected AbstractPointFigure(Point2D location, double radius) {
        this.location = new Point2D.Double(location.getX(), location.getY());
        this.radius = radius;
    }

    public double getX() {
        return location.x;
    }

    public double getY() {
        return location.y;
    }

    public Point2D getLocation() {
        return (Point2D) location.clone();
    }

    public void setLocation(double x, double y) {
        location.setLocation(x, y);
        fireFigureChanged();
    }

    @Override
    public final Rank getRank() {
        return Rank.PUNCTUAL;
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(location.getX() - radius, location.getY() - radius, 2 * radius, 2 * radius);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        double dx = location.getX() - point.getX();
        double dy = location.getY() - point.getY();
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public void scale(Point2D point, double sx, double sy) {
        // todo
    }

    @Override
    public void rotate(Point2D point, double theta) {
        // todo
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
    }

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

            drawPointSymbol(g);

        } finally {
            g.setTransform(oldTransform);
        }
    }

    protected abstract void drawPointSymbol(Graphics2D g);
}