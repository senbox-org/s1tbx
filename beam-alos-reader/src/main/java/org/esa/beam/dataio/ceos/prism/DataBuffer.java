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
