package org.esa.beam.framework.datamodel;

import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class ImageGeometryTest {
    @Test
    public void testImageToMapTransform() throws Exception {

        // i2m := easting + pixelSize * (pixelX - referencePixelX)

        assertEquals(x(0), i2m(0.0, 0.0, 0.1, x(0.0)));

        assertEquals(x(-1), i2m(5, 0, 0.1, x(-5)));
        assertEquals(x(-0.7), i2m(5, 0, 0.1, x(-2)));
        assertEquals(x(-0.6), i2m(5, 0, 0.1, x(-1)));
        assertEquals(x(-0.5), i2m(5, 0, 0.1, x(0)));
        assertEquals(x(-0.4), i2m(5, 0, 0.1, x(1)));
        assertEquals(x(-0.3), i2m(5, 0, 0.1, x(2)));
        assertEquals(x(0), i2m(5, 0, 0.1, x(5)));

        assertEquals(x(2.5), i2m(0, 3, 0.1, x(-5)));
        assertEquals(x(2.8), i2m(0, 3, 0.1, x(-2)));
        assertEquals(x(2.9), i2m(0, 3, 0.1, x(-1)));
        assertEquals(x(3.0), i2m(0, 3, 0.1, x(0)));
        assertEquals(x(3.1), i2m(0, 3, 0.1, x(1)));
        assertEquals(x(3.2), i2m(0, 3, 0.1, x(2)));
        assertEquals(x(3.5), i2m(0, 3, 0.1, x(5)));

        assertEquals(x(3 + 0.1 * (-2 - 5)), i2m(5, 3, 0.1, x(-2)));
        assertEquals(x(3 + 0.1 * (-1 - 5)), i2m(5, 3, 0.1, x(-1)));
        assertEquals(x(3 + 0.1 * (0 - 5)), i2m(5, 3, 0.1, x(0)));
        assertEquals(x(3 + 0.1 * (1 - 5)), i2m(5, 3, 0.1, x(1)));
        assertEquals(x(3 + 0.1 * (2 - 5)), i2m(5, 3, 0.1, x(2)));
    }

    private static Point2D.Double x(double x) {
        return new Point2D.Double(x, 0.0);
    }

    private static Point2D i2m(double referencePixelX,
                               double easting,
                               double pixelSize,
                               Point2D p) {
        return i2m(referencePixelX, easting, pixelSize).transform(p, null);
    }

    private static AffineTransform i2m(double referencePixelX,
                                       double easting,
                                       double pixelSize) {
        double referencePixelY = 0;
        double pixelSizeX = pixelSize;
        double pixelSizeY = pixelSize;
        double orientation = 0.0;
        double northing = 0.0;

        return ImageGeometry.createImageToMapTransform(referencePixelX,
                                                       referencePixelY,
                                                       easting,
                                                       northing,
                                                       pixelSizeX,
                                                       pixelSizeY,
                                                       orientation);
    }

}
