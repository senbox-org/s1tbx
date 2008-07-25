package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class DefaultViewport implements Viewport {

    private final Rectangle bounds;
    private final AffineTransform viewToModelTransform;
    private final AffineTransform modelToViewTransform;
    private double rotation;
    private final ArrayList<ViewportListener> changeListeners;

    public DefaultViewport() {
        this(new Rectangle());
    }

    public DefaultViewport(Rectangle bounds) {
        Assert.notNull(bounds, "bounds");
        this.bounds = new Rectangle(bounds);
        this.viewToModelTransform = new AffineTransform();
        this.modelToViewTransform = new AffineTransform();
        this.changeListeners = new ArrayList<ViewportListener>(3);
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    public void setBounds(Rectangle bounds) {
        Assert.notNull(bounds, "bounds");
        this.bounds.setRect(bounds);
        fireViewportChanged();
    }

    public AffineTransform getViewToModelTransform() {
        return new AffineTransform(viewToModelTransform);
    }

    public AffineTransform getModelToViewTransform() {
        return new AffineTransform(modelToViewTransform);
    }

    private double getModelScale() {
        return getScale(viewToModelTransform);
    }

    private void setModelScale(double modelScale, Point2D viewCenter) {
        Assert.notNull(viewCenter, "viewCenter");
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
        fireViewportChanged();
    }

    public double getRotationAngle() {
        return rotation;
    }

    public void setRotationAngle(double theta, Point2D viewCenter) {
        Assert.notNull(viewCenter, "viewCenter");
        final AffineTransform t = viewToModelTransform;
        t.translate(viewCenter.getX(), viewCenter.getY());
        t.rotate(theta - rotation);
        t.translate(-viewCenter.getX(), -viewCenter.getY());
        updateModelToViewTransform();
        rotation = theta;
        fireViewportChanged();
    }

    public void move(double deltaX, double deltaY) {
        viewToModelTransform.translate(-deltaX, -deltaY);
        updateModelToViewTransform();
        fireViewportChanged();
    }


    public double getZoomFactor() {
        return 1.0 / getModelScale();
    }

    public void setZoomFactor(double zoomFactor) {
        final Rectangle bounds = getBounds();
        final Point2D.Double viewCenter = new Point2D.Double(bounds.x + 0.5 * bounds.width,
                                                             bounds.y + 0.5 * bounds.height);
        setZoomFactor(zoomFactor, viewCenter);
    }

    public void setZoomFactor(double zoomFactor, Point2D viewCenter) {
        Assert.notNull(viewCenter, "viewCenter");
        setModelScale(1.0 / zoomFactor, viewCenter);
    }


    public void addListener(ViewportListener listener) {
        Assert.notNull(listener, "listener");
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeListener(ViewportListener listener) {
        changeListeners.remove(listener);
    }

    public ViewportListener[] getListeners() {
        return changeListeners.toArray(new ViewportListener[changeListeners.size()]);
    }

    protected void fireViewportChanged() {
        for (ViewportListener listener : getListeners()) {
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