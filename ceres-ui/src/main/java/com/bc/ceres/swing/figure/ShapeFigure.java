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

import java.awt.*;


/**
 * A figure which uses an arbitrary geometry represented by a Java AWT shape.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface ShapeFigure extends Figure {
    /**
     * Gets the shape (geometry) for this figure.
     *
     * @return The shape in model coordinates.
     */
    Shape getShape();

    /**
     * Sets the shape (geometry) for this figure.
     *
     * @param shape The shape in model coordinates.
     */
    void setShape(Shape shape);
}