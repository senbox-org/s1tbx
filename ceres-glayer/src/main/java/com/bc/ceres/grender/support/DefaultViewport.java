package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class DefaultViewport implements Viewport {

    private final Rectangle bounds;
    private final AffineTransform viewToModelTransform;
    private final AffineTransform modelToViewTransform;
    private double orientation;
    private final ArrayList<ViewportListener> changeListeners;
    private double maxZoomFactor;

    public DefaultViewport() {
        this(new Rectangle());
    }

    public DefaultViewport(Rectangle bounds) {
        Assert.notNull(bounds, "bounds");
        this.bounds = new Rectangle(bounds);
        this.viewToModelTransform = new AffineTransform();
        this.modelToViewTransform = new AffineTransform();
        this.changeListeners = new ArrayList<ViewportListener>(3);
        this.maxZoomFactor = 32;
    }

    public double getMaxZoomFactor() {
        return maxZoomFactor;
    }

    public void setMaxZoomFactor(double maxZoomFactor) {
        this.maxZoomFactor = maxZoomFactor;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    public void setBounds(Rectangle bounds) {
        Assert.notNull(bounds, "bounds");
        this.bounds.setRect(bounds);
        fireViewportChanged(false);
    }

    public AffineTransform getViewToModelTransform() {
        return new AffineTransform(viewToModelTransform);
    }

    public AffineTransform getModelToViewTransform() {
        return new AffineTransform(modelToViewTransform);
    }

    public double getOrientation() {
        return orientation;
    }

    public double getZoomFactor() {
        return getScale(modelToViewTransform);
    }

    public void rotate(double orientation) {
        rotate(orientation, getViewportCenterPoint());
    }

    public void rotate(double orientation, Point2D viewCenter) {
        Assert.notNull(viewCenter, "viewCenter");
        if (this.orientation != orientation) {
            final AffineTransform v2m = viewToModelTransform;
            v2m.translate(viewCenter.getX(), viewCenter.getY());
            v2m.rotate(orientation - this.orientation);
            v2m.translate(-viewCenter.getX(), -viewCenter.getY());
            updateModelToViewTransform();
            this.orientation = orientation;
            fireViewportChanged(true);
        }
    }

    public void move(double modelPosX, double modelPosY) {
        viewToModelTransform.setTransform(viewToModelTransform.getScaleX(),
                                          viewToModelTransform.getShearY(),
                                          viewToModelTransform.getShearX(),
                                          viewToModelTransform.getScaleY(),
                                          modelPosX,
                                          modelPosY);
        updateModelToViewTransform();
        fireViewportChanged(false);
    }

    public void moveViewDelta(double deltaX, double deltaY) {
        viewToModelTransform.translate(-deltaX, -deltaY);
        updateModelToViewTransform();
        fireViewportChanged(false);
    }

    public void zoom(Rectangle2D modelArea) {
        final double viewportWidth = bounds.width;
        final double viewportHeight = bounds.height;
        final double zoomFactor = Math.min(viewportWidth / modelArea.getWidth(),
                                           viewportHeight / modelArea.getHeight());
        zoom(modelArea.getCenterX(),
             modelArea.getCenterY(),
             zoomFactor);
    }

    public void zoom(double modelCenterX, double modelCenterY, double zoomFactor) {
        zoomFactor = limitZoomFactor(zoomFactor);
        final double viewportWidth = bounds.width;
        final double viewportHeight = bounds.height;
        final double modelOffsetX = modelCenterX - 0.5 * viewportWidth / zoomFactor;
        final double modelOffsetY = modelCenterY - 0.5 * viewportHeight / zoomFactor;
        final double orientation = getOrientation();
        final AffineTransform m2v = new AffineTransform();
        m2v.scale(zoomFactor, zoomFactor);
        m2v.translate(-modelOffsetX, -modelOffsetY);
        modelToViewTransform.setTransform(m2v);
        updateViewToModelTransform();
        this.orientation = 0;
        rotate(orientation);
        fireViewportChanged(false);
    }

    private Point2D.Double getViewportCenterPoint() {
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    public void zoom(double zoomFactor) {
        zoom(zoomFactor, getViewportCenterPoint());
    }

    public void zoom(double zoomFactor, Point2D viewCenter) {
        Assert.notNull(viewCenter, "viewCenter");
        zoomFactor = limitZoomFactor(zoomFactor);
        final AffineTransform v2m = viewToModelTransform;
        final double m00 = v2m.getScaleX();
        final double m10 = v2m.getShearY();
        final double m01 = v2m.getShearX();
        final double m11 = v2m.getScaleY();
        final double sx = Math.sqrt(m00 * m00 + m10 * m10);
        final double sy = Math.sqrt(m01 * m01 + m11 * m11);
        v2m.translate(viewCenter.getX(), viewCenter.getY());
        v2m.scale(1.0 / zoomFactor / sx, 1.0 / zoomFactor / sy);  // preserves signum & rotation of m00, m01, m10 and m11
        v2m.translate(-viewCenter.getX(), -viewCenter.getY());
        updateModelToViewTransform();
        fireViewportChanged(false);
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


    protected void fireViewportChanged(final boolean orientationChanged) {
        for (ViewportListener listener : getListeners()) {
            listener.handleViewportChanged(this, orientationChanged);
        }
    }

    private void updateModelToViewTransform() {
        try {
            modelToViewTransform.setTransform(viewToModelTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateViewToModelTransform() {
        try {
            viewToModelTransform.setTransform(modelToViewTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[viewToModelTransform=" + viewToModelTransform + "]";
    }

    // todo - move somewhere else
    public static double getScale(AffineTransform t) {
        final double m00 = t.getScaleX();
        final double m10 = t.getShearY();
        return m10 == 0.0 ? Math.abs(m00) : Math.sqrt(m00 * m00 + m10 * m10);
    }

    private double limitZoomFactor(double viewScale) {
        return limitZoomFactor(viewScale, maxZoomFactor);
    }

    public static double limitZoomFactor(double viewScale, double viewScaleMax) {
        if (viewScaleMax > 1.0) {
            final double upperLimit = viewScaleMax;
            final double lowerLimit = 1.0 / upperLimit;
            if (viewScale < lowerLimit) {
                viewScale = lowerLimit;
            } else if (viewScale > upperLimit) {
                viewScale = upperLimit;
            }
        }
        return viewScale;
    }

}