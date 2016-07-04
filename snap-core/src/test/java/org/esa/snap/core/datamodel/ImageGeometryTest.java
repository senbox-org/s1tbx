package org.esa.snap.core.datamodel;

import org.geotools.referencing.CRS;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void testPixelSizeCalculationWithTiePointGeoCoding() throws Exception {
        Product product = new Product("test", "T", 50, 50);
        TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0.5, 0.5, 50, 50, new float[]{50f, 50f, 45f, 45f});
        product.addTiePointGrid(latGrid);
        TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0.5, 0.5, 50, 50, new float[]{10f, 15f, 10f, 15f});
        product.addTiePointGrid(lonGrid);
        latGrid.setSynthetic(true);
        lonGrid.setSynthetic(true);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        ImageGeometry imageGeometry = ImageGeometry.createTargetGeometry(product, product.getSceneGeoCoding().getMapCRS(),
                                                                         null, null, null, null, null, null, null, null, null);

        assertEquals(0.1, imageGeometry.getPixelSizeX(), 1.0e-6);
        assertEquals(0.1, imageGeometry.getPixelSizeY(), 1.0e-6);
    }

    @Test
    public void testMapBoundaryWithTiePointGeoCoding() throws Exception {
        Product product = new Product("test", "T", 50, 50);
        TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0.5, 0.5, 50, 50, new float[]{50f, 50f, 45f, 45f});
        product.addTiePointGrid(latGrid);
        TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0.5, 0.5, 50, 50, new float[]{10f, 15f, 10f, 15f});
        product.addTiePointGrid(lonGrid);
        latGrid.setSynthetic(true);
        lonGrid.setSynthetic(true);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        Rectangle2D mapBoundary = ImageGeometry.createMapBoundary(product, product.getSceneGeoCoding().getMapCRS());

        assertEquals(9.95, mapBoundary.getMinX(), 1.0e-2);
        assertEquals(14.95, mapBoundary.getMaxX(), 1.0e-2);
        assertEquals(45.05, mapBoundary.getMinY(), 1.0e-2);
        assertEquals(50.05, mapBoundary.getMaxY(), 1.0e-2);
    }


    @Test
    public void testPixelSizeCalculationWithPixelGeoCoding() throws Exception {
        Product product = new Product("test", "T", 2, 2);
        Band latBand = product.addBand("lat", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lon", ProductData.TYPE_FLOAT32);
        latBand.setSynthetic(true);
        lonBand.setSynthetic(true);
        // these are the outer bounds
        latBand.setRasterData(ProductData.createInstance(new float[]{49.5f, 49.5f, 45.5f, 45.5f}));
        lonBand.setRasterData(ProductData.createInstance(new float[]{10.5f, 14.5f, 10.5f, 14.5f}));
        product.setSceneGeoCoding(new PixelGeoCoding2(latBand, lonBand, null, 2));

        ImageGeometry imageGeometry = ImageGeometry.createTargetGeometry(product, product.getSceneGeoCoding().getMapCRS(),
                                                                         null, null, null, null, null, null, null, null, null);
        assertEquals(2.0, imageGeometry.getPixelSizeX(), 1.0e-6);
        assertEquals(2.0, imageGeometry.getPixelSizeY(), 1.0e-6);
    }

    @Test
    public void testMapBoundaryWithPixelGeoCoding() throws Exception {
        Product product = new Product("test", "T", 2, 2);
        Band latBand = product.addBand("lat", ProductData.TYPE_FLOAT32);
        Band lonBand = product.addBand("lon", ProductData.TYPE_FLOAT32);
        latBand.setSynthetic(true);
        lonBand.setSynthetic(true);
        // these are the outer bounds
        latBand.setRasterData(ProductData.createInstance(new float[]{49.5f, 49.5f, 45.5f, 45.5f}));
        lonBand.setRasterData(ProductData.createInstance(new float[]{10.5f, 14.5f, 10.5f, 14.5f}));
        product.setSceneGeoCoding(new PixelGeoCoding2(latBand, lonBand, null, 2));

        Rectangle2D mapBoundary = ImageGeometry.createMapBoundary(product, product.getSceneGeoCoding().getMapCRS());

        assertEquals(10.5, mapBoundary.getMinX(), 1.0e-6);
        assertEquals(14.5, mapBoundary.getMaxX(), 1.0e-6);
        assertEquals(45.5, mapBoundary.getMinY(), 1.0e-6);
        assertEquals(49.5, mapBoundary.getMaxY(), 1.0e-6);
    }

    @Test
    public void testPixelSizeCalculationWithCrsGeoCoding() throws Exception {
        Product product = new Product("test", "T", 50, 50);
        product.addBand("dummy", ProductData.TYPE_FLOAT32);
        product.setSceneGeoCoding(new CrsGeoCoding(CRS.decode("EPSG:32632"), 50, 50, 0, 0, 30, 30, 0, 0));

        ImageGeometry imageGeometry = ImageGeometry.createTargetGeometry(product, product.getSceneGeoCoding().getMapCRS(),
                                                                         null, null, null, null, null, null, null, null, null);

        assertEquals(30, imageGeometry.getPixelSizeX(), 1.0e-6);
        assertEquals(30, imageGeometry.getPixelSizeY(), 1.0e-6);
    }

    @Test
    public void testMapBoundaryWithCrsGeoCoding() throws Exception {
        Product product = new Product("test", "T", 50, 50);
        product.addBand("dummy", ProductData.TYPE_FLOAT32);
        product.setSceneGeoCoding(new CrsGeoCoding(CRS.decode("EPSG:32632"), 50, 50, 0, 0, 30, 30, 0, 0));

        Rectangle2D mapBoundary = ImageGeometry.createMapBoundary(product, product.getSceneGeoCoding().getMapCRS());

        assertEquals(0, mapBoundary.getMinX(), 1.0e-6);
        assertEquals(50 * 30, mapBoundary.getMaxX(), 1.0e-6);
        assertEquals(-50*30, mapBoundary.getMinY(), 1.0e-6);
        assertEquals(0, mapBoundary.getMaxY(), 1.0e-6);
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

        return ImageGeometry.createImageToMapTransform(referencePixelX,
                                                       0.0,
                                                       easting,
                                                       0.0,
                                                       pixelSize,
                                                       pixelSize,
                                                       0.0);
    }

}
