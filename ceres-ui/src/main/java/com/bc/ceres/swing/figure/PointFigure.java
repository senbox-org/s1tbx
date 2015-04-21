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

import java.awt.geom.Point2D;


/**
 * A point figure.
 * <p>
 * Clients should not implement this interface directly, because it may change in the future.
 * Instead they should derive their {@code Figure} implementation from {@link AbstractPointFigure}.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since Ceres 0.10
 */
public interface PointFigure extends Figure {
    /**
     * @return The X-coordinate of the current location in model coordinates.
     */
    double getX();

    /**
     * @return The Y-coordinate of the current location in model coordinates.
     */
    double getY();

    /**
     * @return The current location in model coordinates.
     */
    Point2D getLocation();

    /**
     * @param location The current location in model coordinates.
     */
    void setLocation(Point2D location);

    /**
     * @return The symbol that is used to represent the figure.
     */
    Symbol getSymbol();
}