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
package org.esa.snap.core.util;

import java.awt.Rectangle;

/**
 * Extends a rectangle using a horizontal and vertical extension.
 * The rectangle is limited by the given clipping rectangle
 */
public class RectangleExtender {

    private final Rectangle clippingRect;
    private final int widthExtend;
    private final int heightExtend;

    /**
     * Creates a RectangleExtender
     * 
     * @param clippingRect  The clipping Rectangle.
     * @param widthExtend   The horizontal extension
     * @param heightExtend  The vertical extension
     */
    public RectangleExtender(Rectangle clippingRect, int widthExtend, int heightExtend) {
        this.clippingRect = clippingRect;
        this.widthExtend = widthExtend;
        this.heightExtend = heightExtend;
    }

    /**
     * Extends the given rectangle and clips the result to the clipping rectangle.
     * @param rectangle
     * 
     * @return extended rectangle
     */
    public Rectangle extend(Rectangle rectangle) {
        Rectangle copy = new Rectangle(rectangle);
        copy.grow(widthExtend, heightExtend);
        return copy.intersection(clippingRect);
    }
}
