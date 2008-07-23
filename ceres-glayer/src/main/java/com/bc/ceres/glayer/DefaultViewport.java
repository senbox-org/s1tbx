package com.bc.ceres.glayer;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class DefaultViewport implements Viewport {
    private static final Point2D.Double P0 = new Point2D.Double(0, 0);

    private final ArrayList<ChangeListener> changeListeners;
    private final AffineTransform viewToModelTransform;
    private final AffineTransform modelToViewTransform;
    private double currentRotation;

    public DefaultViewport() {
        this.changeListeners = new ArrayList<ChangeListener>(3);
        this.viewToModelTransform = new AffineTransform();
        this.modelToViewTransform = new AffineTransform();
    }

    public AffineTransform getViewToModelTransform() {
        return new AffineTransform(viewToModelTransform);
    }

    public AffineTransform getModelToViewTransform() {
        return new AffineTransform(modelToViewTransform);
    }

    public Point2D getModelOffset() {
        return viewToModelTransform.transform(P0, null);
    }

    public void setModelOffset(Point2D newOffset) {
        final AffineTransform t = viewToModelTransform;
        final Point2D oldOffset = getModelOffset();
        final double dx = newOffset.getX() - oldOffset.getX();
        final double dy = newOffset.getY() - oldOffset.getY();
        t.translate(dx, dy);
        updateModelToViewTransform();
        fireChange();
    }

    public double getModelScale() {
        return getScale(viewToModelTransform);
    }

    public void setModelScale(double modelScale, Point2D viewCenter) {
        final AffineTransform t = viewToModelTransform;
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        final double m01 = t.getShearX();
        final double m11 = t.getScaleY();
        final double sx = Math.sqrt(m00 * m00 + m10 * m10);
        final double sy = Math.sqrt(m01 * m01 + m11 * m11);
        t.translate(viewCenter.getX(), viewCenter.getY());
        t.scale(modelScale / sx, modelScale / sy);  // preserves signum & rotation of m00, m01, m10 and m11
        t.translate(-viewCenter.getX(), -viewCenter.getY());
        updateModelToViewTransform();
        fireChange();
    }

    public double getModelRotation() {
        return currentRotation;
    }

    public void setModelRotation(double theta, Point2D viewCenter) {
        final AffineTransform t = viewToModelTransform;
        t.translate(viewCenter.getX(), viewCenter.getY());
        t.rotate(theta - currentRotation);
        t.translate(-viewCenter.getX(), -viewCenter.getY());
        updateModelToViewTransform();
        currentRotation = theta;
        fireChange();
    }

    public void move(double deltaX, double deltaY) {
        viewToModelTransform.translate(-deltaX, -deltaY);
        updateModelToViewTransform();
        fireChange();
    }

    public void addChangeListener(ChangeListener listener) {
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return changeListeners.toArray(new ChangeListener[changeListeners.size()]);
    }

    protected void fireChange() {
        for (ChangeListener listener : getChangeListeners()) {
            listener.handleViewportChanged(this);
        }
    }

    private void updateModelToViewTransform() {
        try {
            modelToViewTransform.setTransform(viewToModelTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[viewToModelTransform=" + viewToModelTransform + "]";
    }

    public static double getScale(AffineTransform t) {
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        return m10 == 0.0 ? Math.abs(m00) : Math.sqrt(m00 * m00 + m10 * m10);
    }
}