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

    @Override
    public Rectangle getViewBounds() {
        return new Rectangle(bounds);
    }

    @Override
    public void setViewBounds(Rectangle bounds) {
        Assert.notNull(bounds, "bounds");
        this.bounds.setRect(bounds);
        fireViewportChanged(false);
    }

    @Override
    public AffineTransform getViewToModelTransform() {
        return new AffineTransform(viewToModelTransform);
    }

    @Override
    public AffineTransform getModelToViewTransform() {
        return new AffineTransform(modelToViewTransform);
    }

    @Override
    public double getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(double orientation) {
        if (this.orientation != orientation) {
            Point2D.Double vc = getViewportCenterPoint();
            final AffineTransform v2m = viewToModelTransform;
            v2m.translate(vc.getX(), vc.getY());
            v2m.rotate(orientation - this.orientation);
            v2m.translate(-vc.getX(), -vc.getY());
            updateModelToViewTransform();
            this.orientation = orientation;
            fireViewportChanged(true);
        }
    }

    @Override
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

    @Override
    public void moveViewDelta(double deltaX, double deltaY) {
    	if (deltaX == 0.0 && deltaY == 0.0) {
    		return;
    	}
    	viewToModelTransform.translate(-deltaX, -deltaY);
    	updateModelToViewTransform();
    	fireViewportChanged(false);
    }

    @Override
    public double getZoomFactor() {
        return Math.sqrt(modelToViewTransform.getDeterminant());
    }

    @Override
    public void zoom(double zoomFactor) {
        Point2D.Double vc = getViewportCenterPoint();
        AffineTransform v2m = viewToModelTransform;
        double s = Math.sqrt(v2m.getDeterminant());
        v2m.translate(vc.getX(), vc.getY());
        v2m.scale(1.0 / zoomFactor / s, 1.0 / zoomFactor / s);
        v2m.translate(-vc.getX(), -vc.getY());
        updateModelToViewTransform();
        fireViewportChanged(false);
    }

    @Override
    public void zoom(Rectangle2D modelArea) {
        final double viewportWidth = bounds.width;
        final double viewportHeight = bounds.height;
        final double zoomFactor = Math.min(viewportWidth / modelArea.getWidth(),
                                           viewportHeight / modelArea.getHeight());
        zoom(modelArea.getCenterX(),
             modelArea.getCenterY(),
             zoomFactor);
    }

    @Override
    public void zoom(double modelCenterX, double modelCenterY, double zoomFactor) {
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
        setOrientation(orientation);
        fireViewportChanged(false);
    }

    private Point2D.Double getViewportCenterPoint() {
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    @Override
    public void addListener(ViewportListener listener) {
        Assert.notNull(listener, "listener");
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    @Override
    public void removeListener(ViewportListener listener) {
        changeListeners.remove(listener);
    }

    @Override
    public ViewportListener[] getListeners() {
        return changeListeners.toArray(new ViewportListener[changeListeners.size()]);
    }
    
    @Override
    public void synchronizeWith(Viewport other) {
        modelToViewTransform.setTransform(other.getModelToViewTransform());
        viewToModelTransform.setTransform(other.getViewToModelTransform());
        final boolean orientationChange = (orientation != other.getOrientation());
        if (orientationChange) {
            orientation = other.getOrientation();
        }
        fireViewportChanged(orientationChange);
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
}