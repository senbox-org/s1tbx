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

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Only implements abstract Figure methods, no overrides!
 */
class AbstractFigureImpl extends AbstractFigure {

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public void draw(Rendering rendering) {
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle();
    }

    @Override
    public Rank getRank() {
        return Rank.POINT;
    }

    void postChangeEvent() {
        fireFigureChanged();
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        return false;
    }

    @Override
    public FigureStyle getNormalStyle() {
        return new DefaultFigureStyle();
    }

    @Override
    public FigureStyle getSelectedStyle() {
        return new DefaultFigureStyle();
    }
}