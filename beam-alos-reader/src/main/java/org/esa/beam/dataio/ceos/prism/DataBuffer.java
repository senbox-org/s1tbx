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

package org.esa.beam.dataio.ceos.prism;

import org.esa.beam.framework.datamodel.ProductData;

import java.awt.*;

class DataBuffer {

    private final ProductData _buffer;
    private final Point _location;
    private final Dimension _dimension;

    DataBuffer(final ProductData buffer, final int x, final int y, final int width, final int height) {
        _buffer = buffer;
        _location = new Point(x, y);
        _dimension = new Dimension(width, height);
    }

    ProductData getBuffer() {
        return _buffer;
    }

    Point getLocation() {
        return _location;
    }

    Dimension getDimension() {
        return _dimension;
    }
}
