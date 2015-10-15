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

package org.esa.snap.core.datamodel;

import java.awt.geom.Point2D;

/**
 * A <code>PixelPos</code> represents a position or point in a pixel coordinate system.
 */
public class PixelPos extends Point2D.Double {

    /**
     * Constructs and initializes a <code>PixelPos</code> with coordinate (0,&nbsp;0).
     */
    public PixelPos() {
    }

    /**
     * Constructs and initializes a <code>PixelPos</code> with the specified coordinate.
     *
     * @param x the x component of the coordinate
     * @param y the y component of the coordinate
     */
    public PixelPos(double x, double y) {
        super(x, y);
    }

    /**
     * Tests whether or not this pixel position is valid.
     *
     * @return true, if so
     */
    public boolean isValid() {
        return !(java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y));
    }

    /**
     * Sets this pixel position so that is becomes invalid.
     */
    public void setInvalid() {
        x = java.lang.Double.NaN;
        y = java.lang.Double.NaN;
    }
}
