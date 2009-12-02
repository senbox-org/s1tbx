package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportOwner;

import java.awt.Component;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public abstract class ViewportInteractor extends AbstractInteractor {

    protected ViewportInteractor() {
    }

    protected Viewport getViewport(InputEvent inputEvent) {
        final Component component = inputEvent.getComponent();
        if (component instanceof ViewportOwner) {
            return ((ViewportOwner) component).getViewport();
        }
        return null;
    }

    protected AffineTransform getViewToModelTransform(InputEvent inputEvent) {
        return getViewport(inputEvent).getViewToModelTransform();
    }

    protected AffineTransform getModelToViewTransform(InputEvent inputEvent) {
        return getViewport(inputEvent).getModelToViewTransform();
    }

    protected Point2D toModelPoint(MouseEvent mouseEvent) {
        return toModelPoint(mouseEvent, mouseEvent.getPoint());
    }

    protected Point2D toModelPoint(InputEvent inputEvent, Point2D point) {
        return getViewToModelTransform(inputEvent).transform(point, null);
    }

    protected Shape toModelShape(InputEvent inputEvent, Shape shape) {
        return getViewToModelTransform(inputEvent).createTransformedShape(shape);
    }

}