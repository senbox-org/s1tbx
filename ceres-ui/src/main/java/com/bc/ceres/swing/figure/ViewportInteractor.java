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

package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportAware;

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
        return getViewport(inputEvent.getComponent());
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

    private Viewport getViewport(Component component) {
        while (true) {
            if (component instanceof ViewportAware) {
                return ((ViewportAware) component).getViewport();
            } else if (component instanceof Viewport) {
                return (Viewport) component;
            } else if (component == null || component.getParent() == null) {
                return null;
            }
            component = component.getParent();
        }
    }

}