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

import com.bc.ceres.binding.PropertySet;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

/**
 * A {@link PropertySet} that provides style information for figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureStyle extends PropertySet {

    String getName();

    /**
     * Gets the effective stroke style used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective stroke style used for drawing.
     */
    Stroke getStroke();

    /**
     * Gets the effective stroke style used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @param scale The current model-to-view scaling.
     * @return The effective stroke style used for drawing.
     */
    Stroke getStroke(double scale);

    /**
     * Gets the effective stroke paint used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective stroke paint used for drawing.
     */
    Paint getStrokePaint();

    Color getStrokeColor();

    double getStrokeOpacity();

    double getStrokeWidth();

    /**
     * Gets the effective fill paint used for drawing the interior of a polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective fill paint used for drawing.
     */
    Paint getFillPaint();

    Color getFillColor();

    double getFillOpacity();

    /**
     * Gets the effective point symbol used for drawing of points figures.
     *
     * @return The effective point symbol used for drawing.
     */
    Symbol getSymbol();

    String getSymbolName();

    String getSymbolImagePath();

    double getSymbolRefX();

    double getSymbolRefY();

    String toCssString();

    void fromCssString(String css);
}
