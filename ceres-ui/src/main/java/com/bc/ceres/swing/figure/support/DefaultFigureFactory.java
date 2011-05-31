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

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.PointFigure;
import com.bc.ceres.swing.figure.ShapeFigure;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;

public class DefaultFigureFactory implements FigureFactory {
    @Override
    public PointFigure createPointFigure(Point2D geometry, FigureStyle style) {
        return new DefaultPointFigure(geometry, 1E-10, style, deriveSelectedStyle(style));
    }

    @Override
    public ShapeFigure createLineFigure(Shape geometry, FigureStyle style) {
        return new DefaultShapeFigure(geometry, Figure.Rank.LINE, style, deriveSelectedStyle(style));
    }

    @Override
    public ShapeFigure createPolygonFigure(Shape geometry, FigureStyle style) {
        return new DefaultShapeFigure(geometry, Figure.Rank.AREA, style, deriveSelectedStyle(style));
    }

    public FigureStyle deriveSelectedStyle(FigureStyle style) {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setFillColor(style.getFillColor());
        figureStyle.setFillOpacity(style.getFillOpacity());
        figureStyle.setStrokeColor(Color.YELLOW);
        figureStyle.setStrokeOpacity(0.9);
        figureStyle.setStrokeWidth(style.getStrokeWidth() + 1.0);
        figureStyle.setSymbolName(style.getSymbolName());
        figureStyle.setSymbolName(style.getSymbolImagePath());
        figureStyle.setSymbolRefX(style.getSymbolRefX());
        figureStyle.setSymbolRefY(style.getSymbolRefY());
        return figureStyle;
    }
}
