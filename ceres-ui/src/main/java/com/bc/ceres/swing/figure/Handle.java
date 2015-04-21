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

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Handles are shown on figures vertices or boundaries. Usually they when figures are selected.
 * Various handle types may be used to modify a figure's geometry, e.g. resize, rotate or move
 * figures or their vertices.
 * <p>
 * Important note: The {@link #getShape() shape} and {@link #getShape() bounds}
 * returned by handles are in <i>view</i> coordinates. This is in contrast to
 * {@link Figure figures}, which return these properties in <i>model</i> coordinates.
 * <p>
 * Clients should not implement this interface directly, because it may change in the future.
 * Instead they should derive their {@code Handle} implementation from {@link AbstractHandle}.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Handle extends ShapeFigure {

    /**
     * @return The current location in model coordinates.
     */
    Point2D getLocation();

    /**
     * @return The mouse cursor that will appear if users point the mouse over a handle.
     */
    Cursor getCursor();


    /**
     * Gets the shape (geometry) for this figure.
     *
     * @return The shape in <i>view</i> coordinates.
     */
    @Override
    Shape getShape();

    /**
     * Sets the shape (geometry) for this figure.
     *
     * @param shape The shape in <i>view</i> coordinates.
     */
    @Override
    void setShape(Shape shape);

    /**
     * The bounds of the handle.
     *
     * @return The bounds of the handle in <i>view</i> units.
     */
    @Override
    Rectangle2D getBounds();
}
