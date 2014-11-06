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

package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class DefaultViewport implements Viewport {

    private Rectangle viewBounds;
    private AffineTransform modelToViewTransform;
    private AffineTransform viewToModelTransform;
    private boolean modelYAxisDown;
    private double orientation;
    private ArrayList<ViewportListener> changeListeners;

    public DefaultViewport() {
        this(new Rectangle());
    }

    public DefaultViewport(Rectangle viewBounds) {
        this(viewBounds, false);
    }

    public DefaultViewport(boolean modelYAxisDown) {
        this(new Rectangle(), modelYAxisDown);
    }

    public DefaultViewport(Rectangle viewBounds, boolean modelYAxisDown) {
        Assert.notNull(viewBounds, "viewBounds");
        this.viewBounds = new Rectangle(viewBounds);
        this.modelYAxisDown = modelYAxisDown;
        this.modelToViewTransform = AffineTransform.getScaleInstance(1.0, modelYAxisDown ? 1.0 : -1.0);
        this.viewToModelTransform = AffineTransform.getScaleInstance(1.0, modelYAxisDown ? 1.0 : -1.0);
        this.changeListeners = new ArrayList<>(3);
    }

    @Override
    public boolean isModelYAxisDown() {
        return modelYAxisDown;
    }

    @Override
    public void setModelYAxisDown(boolean modelYAxisDown) {
        if (this.modelYAxisDown != modelYAxisDown) {
            this.modelYAxisDown = modelYAxisDown;
            viewToModelTransform.scale(1.0, -1.0);
            modelToViewTransform.scale(1.0, -1.0);
            fireViewportChanged(false);
        }
    }

    @Override
    public Rectangle getViewBounds() {
        return new Rectangle(viewBounds);
    }

    @Override
    public void setViewBounds(Rectangle viewBounds) {
        Assert.notNull(viewBounds, "viewBounds");
        if (!viewBounds.equals(this.viewBounds)) {
            this.viewBounds.setRect(viewBounds);
            fireViewportChanged(false);
        }
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
        Point2D.Double vc = getViewportCenterPoint();
        setOrientation(orientation, vc);
    }

    @Override
    public double getOffsetX() {
        return viewToModelTransform.getTranslateX();
    }

    @Override
    public double getOffsetY() {
        return viewToModelTransform.getTranslateY();
    }

    @Override
    public void setOffset(double offsetX, double offsetY) {
        viewToModelTransform.setTransform(viewToModelTransform.getScaleX(),
                                          viewToModelTransform.getShearY(),
                                          viewToModelTransform.getShearX(),
                                          viewToModelTransform.getScaleY(),
                                          offsetX,
                                          offsetY);
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
        return Math.sqrt(Math.abs(modelToViewTransform.getDeterminant()));
    }

    @Override
    public void setZoomFactor(double zoomFactor) {
        Assert.argument(zoomFactor > 0.0, "zoomFactor > 0.0");
        setZoomFactor(zoomFactor, getViewportCenterPoint());
    }

    @Override
    public void zoom(Rectangle2D modelBounds) {
        final double viewportWidth = viewBounds.width;
        final double viewportHeight = viewBounds.height;
        final double zoomFactor = Math.min(viewportWidth / modelBounds.getWidth(),
                                           viewportHeight / modelBounds.getHeight());
        if (zoomFactor > 0.0) {
            setZoomFactor(zoomFactor, modelBounds.getCenterX(), modelBounds.getCenterY());
//        // useful for debugging - don't delete
//        System.out.println(this.getClass() + " ===============================================");
//        System.out.println("  modelBounds         = " + modelBounds);
//        System.out.println("  computedModelBounds = " + getViewToModelTransform().createTransformedShape(viewBounds).getBounds2D());
//        System.out.println("  viewBounds          = " + viewBounds);
//        System.out.println("  computedViewBounds  = " + getModelToViewTransform().createTransformedShape(modelBounds).getBounds2D());
        }
    }

    @Override
    public void setZoomFactor(double zoomFactor, double modelCenterX, double modelCenterY) {
        Assert.argument(zoomFactor > 0.0, "zoomFactor > 0.0");
        final double sx = 1.0;
        final double sy = modelYAxisDown ? 1.0 : -1.0;
        final double viewportWidth = viewBounds.width;
        final double viewportHeight = viewBounds.height;
        final double modelOffsetX = modelCenterX - 0.5 * sx * viewportWidth / zoomFactor;
        final double modelOffsetY = modelCenterY - 0.5 * sy * viewportHeight / zoomFactor;
        final double orientation = getOrientation();
        // todo - use code similar to setZoomFactor(f, vp) (nf - 21.10.2008)
        final AffineTransform m2v = AffineTransform.getScaleInstance(sx, sy);
        m2v.scale(zoomFactor, zoomFactor);
        m2v.translate(-modelOffsetX, -modelOffsetY);
        modelToViewTransform.setTransform(m2v);
        updateViewToModelTransform();
        this.orientation = 0;
        // todo - this call (hack?) fires an additional change event! Not good! (nf - 21.10.2008)
        setOrientation(orientation);
        fireViewportChanged(false);
    }

    void setZoomFactor(double zoomFactor, Point2D vc) {
        Assert.argument(zoomFactor > 0.0, "zoomFactor > 0.0");
        double oldZoomFactor = getZoomFactor();
        if (oldZoomFactor != zoomFactor) {
            AffineTransform v2m = viewToModelTransform;
            v2m.translate(vc.getX(), vc.getY());
            v2m.scale(oldZoomFactor / zoomFactor, oldZoomFactor / zoomFactor);
            v2m.translate(-vc.getX(), -vc.getY());
            updateModelToViewTransform();
            fireViewportChanged(false);
        }
    }

    private void setOrientation(double orientation, Point2D vc) {
        double oldOrientation = getOrientation();
        if (oldOrientation != orientation) {
            final AffineTransform v2m = viewToModelTransform;
            v2m.translate(vc.getX(), vc.getY());
            v2m.rotate(orientation - oldOrientation);
            v2m.translate(-vc.getX(), -vc.getY());
            updateModelToViewTransform();
            this.orientation = orientation;
            fireViewportChanged(true);
        }
    }


    private Point2D.Double getViewportCenterPoint() {
        return new Point2D.Double(viewBounds.getCenterX(), viewBounds.getCenterY());
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
    public void setTransform(Viewport other) {
        modelToViewTransform.setTransform(other.getModelToViewTransform());
        viewToModelTransform.setTransform(other.getViewToModelTransform());
        modelYAxisDown = other.isModelYAxisDown();
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

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Viewport clone() {
        try {
            DefaultViewport vp = (DefaultViewport) super.clone();
            vp.viewBounds = new Rectangle(viewBounds);
            vp.changeListeners = new ArrayList<>(3);
            return vp;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}