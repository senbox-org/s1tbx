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

import com.bc.ceres.swing.figure.AbstractPointFigure;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.geom.Point2D;

public class DefaultPointFigure extends AbstractPointFigure {

    // Point location in model coordinates.
    private final Point2D.Double location;
    // Point radius in model coordinates.
    private final double radius;

    public DefaultPointFigure(Point2D location, double radius) {
        this(location, radius, DefaultFigureStyle.createFromCss("symbol:star; fill-color:#0000FF; stroke-color:#FFFFFF"));
    }

    public DefaultPointFigure(Point2D location, double radius, FigureStyle style) {
        this(location, radius, style, style);
    }

    public DefaultPointFigure(Point2D location, double radius, FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
        this.location = new Point2D.Double(location.getX(), location.getY());
        this.radius = radius;
        setSelectable(true);
    }

    @Override
    public double getX() {
        return location.x;
    }

    @Override
    public double getY() {
        return location.y;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public void setLocation(double x, double y) {
        location.setLocation(x, y);
        fireFigureChanged();
    }
}