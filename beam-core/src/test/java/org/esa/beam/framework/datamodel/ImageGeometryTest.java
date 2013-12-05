package org.esa.beam.framework.datamodel;

import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

public class ImageGeometryTest {

    @Test
    public void testCreateValidRect() {
        int width = 100;
        int height = 100;
        Product product = new Product("product", "type", width, height);
        Band latBand = new Band("lat", ProductData.TYPE_FLOAT32, width, height);
        Band lonBand = new Band("lon", ProductData.TYPE_FLOAT32, width, height);
        float[] latData = new float[width * height];
        float[] lonData = new float[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                float latValue = Float.NaN;
                float lonValue = Float.NaN;
                if (i >= width / 4 && i <= 3 * (width / 4) &&
                        j >= height / 4 && j <= 3 * (height / 4)) {
                    latValue = i;
                    lonValue = j;
                }
                latData[width * i + j] = latValue;
                lonData[height * i + j] = lonValue;
            }
        }
        latBand.setDataElems(latData);
        lonBand.setDataElems(lonData);
        latBand.setNoDataValue(Float.NaN);
        latBand.setNoDataValueUsed(true);
        lonBand.setNoDataValue(Float.NaN);
        lonBand.setNoDataValueUsed(true);
        product.addBand(latBand);
        product.addBand(lonBand);
        product.setGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 5));

        final Rectangle2D rect = ImageGeometry.createValidRect(product);

        assertEquals(width / 4 + 0.5, rect.getX(), 0);
        assertEquals(height / 4 + 0.5, rect.getY(), 0);
        assertEquals(width * 3 / 4 + 0.5, rect.getX() + rect.getWidth(), 0);
        assertEquals(height * 3 / 4 + 0.5, rect.getY() + rect.getHeight(), 0);
        for (int x = (int) rect.getX(); x < rect.getX() + rect.getWidth(); x++) {
            for (int y = (int) rect.getY(); y < rect.getY() + rect.getHeight(); y++) {
                assertNotSame(latBand.getNoDataValue(), latBand.getSampleFloat(x, y));
            }
        }
        boolean leftColumnContainsNoDataValues = true;
        boolean rightColumnContainsNoDataValues = true;
        for (int y = (int) rect.getY(); y < rect.getY() + rect.getHeight(); y++) {
            if (latBand.getSampleFloat((int) rect.getX() - 1, y) == latBand.getNoDataValue()) {
                leftColumnContainsNoDataValues = false;
            }
            if (latBand.getSampleFloat((int) (rect.getX() + rect.getWidth()), y) == latBand.getNoDataValue()) {
                rightColumnContainsNoDataValues = false;
            }
        }
        assertTrue(leftColumnContainsNoDataValues);
        assertTrue(rightColumnContainsNoDataValues);
        boolean upperLineContainsNoDataValues = true;
        boolean lowerLineContainsNoDataValues = true;
        for (int x = (int) rect.getX(); x < rect.getX() + rect.getWidth(); x++) {
            if (latBand.getSampleFloat(x, (int) rect.getY() - 1) == latBand.getNoDataValue()) {
                upperLineContainsNoDataValues = false;
            }
            if (latBand.getSampleFloat(x, (int) (rect.getY() + rect.getHeight())) == latBand.getNoDataValue()) {
                lowerLineContainsNoDataValues = false;
            }
        }
        assertTrue(upperLineContainsNoDataValues);
        assertTrue(lowerLineContainsNoDataValues);
    }

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
