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

import com.bc.ceres.swing.figure.AbstractShapeFigure;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.Shape;
import java.awt.geom.Path2D;

public class DefaultShapeFigure extends AbstractShapeFigure {
    private Shape shape;

    public DefaultShapeFigure() {
        this(null, Rank.AREA, new DefaultFigureStyle());
    }

    public DefaultShapeFigure(Shape shape, Rank rank, FigureStyle normalStyle) {
        this(shape, rank, normalStyle, normalStyle);
    }

    public DefaultShapeFigure(Shape shape, Rank rank, FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(rank, normalStyle, selectedStyle);
        this.shape = shape;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape path) {
        shape = path;
        fireFigureChanged();
    }

    @Override
    public DefaultShapeFigure clone() {
        DefaultShapeFigure copy = (DefaultShapeFigure) super.clone();
        copy.shape = new Path2D.Double(shape);
        return copy;
    }
}
