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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.PointFigure;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.bc.ceres.swing.figure.support.StyleDefaults.POINT_HANDLE_SIZE;


/**
 * A {@link com.bc.ceres.swing.figure.Handle Handle} that can be used to change point positions.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public class PointHandle extends AbstractHandle {

    public PointHandle(PointFigure figure, FigureStyle handleStyle) {
        this(figure, handleStyle, createHandleShape(handleStyle));
    }

    public PointHandle(PointFigure figure, FigureStyle handleStyle, Shape shape) {
        super(figure, handleStyle, handleStyle);
        updateLocation();
        setShape(shape);
    }

    @Override
    public PointFigure getFigure() {
        return (PointFigure) super.getFigure();
    }

    @Override
    public void updateLocation() {
        setLocation(getFigure().getLocation());
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
        getFigure().setLocation(getLocation());
    }

    private static Shape createHandleShape(FigureStyle handleStyle) {
        double size = POINT_HANDLE_SIZE;
        Object sizeObj = handleStyle.getValue("size");
        if (sizeObj instanceof Number) {
             size = ((Number) sizeObj).doubleValue();
        }
        return new Ellipse2D.Double(-0.5 * size, -0.5 * size, size, size);
    }
}