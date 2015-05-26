package org.esa.snap.util;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.SceneRasterTransformException;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class SceneRasterTransformUtilsTest {

    private Band band2;
    private Band band1;
    private Product product;
    private Band band3;

    @Before
    public void setUp() {
        product = new Product("A", "B", 4, 8);
        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 4, 8, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 4, 8, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        TiePointGrid lat2 = new TiePointGrid("lat2", 2, 2, 0, 0, 8, 16, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon2 = new TiePointGrid("lon2", 2, 2, 0, 0, 8, 16, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat2);
        product.addTiePointGrid(lon2);
        band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 4, 8, "X");
        band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 8, 16, "Y");
        product.addBand(band1);
        product.addBand(band2);
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        TiePointGrid lat3 = new TiePointGrid("lat3", 2, 2, 0, 0, 2, 4, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon3 = new TiePointGrid("lon3", 2, 2, 0, 0, 2, 4, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat3);
        product.addTiePointGrid(lon3);
        band3 = new VirtualBand("B3", ProductData.TYPE_INT16, 2, 4, "Y");
        product.addBand(band3);
        band3.setGeoCoding(new TiePointGeoCoding(lat3, lon3));
    }

    @Test
    public void testTransformToProductGrid_PixelPos() throws TransformException, SceneRasterTransformException {
        for (double y = 0.5; y < 8; y++) {
            for (double x = 0.5; x < 4; x++) {
                final PixelPos pixelPos = new PixelPos(x, y);
                final PixelPos productPos = SceneRasterTransformUtils.transformToProductRaster(band1, pixelPos);
                Assert.assertEquals(pixelPos.getX(), productPos.getX(), 1e-8);
                Assert.assertEquals(pixelPos.getY(), productPos.getY(), 1e-8);
            }
        }
        for (double y = 0; y < 4; y += 0.5) {
            for (double x = 0; x < 2; x += 0.5) {
                final PixelPos pixelPos = new PixelPos(x, y);
                final PixelPos productPos = SceneRasterTransformUtils.transformToProductRaster(band3, pixelPos);
                Assert.assertEquals(pixelPos.getX() * 2, productPos.getX(), 1e-8);
                Assert.assertEquals(pixelPos.getY() * 2, productPos.getY(), 1e-8);
            }
        }
    }

    @Test
    public void testTransformToRasterGrid_PixelPos() throws TransformException, SceneRasterTransformException {
        for (double y = 0; y < 8; y++) {
            for (double x = 0; x < 4; x++) {
                final PixelPos pixelPos = new PixelPos(x, y);
                final PixelPos productPos = SceneRasterTransformUtils.transformToRasterDataNodeRaster(band3, pixelPos);
                Assert.assertEquals(pixelPos.getX() / 2, productPos.getX(), 1e-8);
                Assert.assertEquals(pixelPos.getY() / 2, productPos.getY(), 1e-8);
            }
        }
    }

    @Test
    public void testTransformFromToRasterGrid_PixelPos() throws TransformException, SceneRasterTransformException {
        TiePointGrid lat4 = new TiePointGrid("lat4", 2, 2, 0, 0, 3, 10, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon4 = new TiePointGrid("lon4", 2, 2, 0, 0, 3, 10, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat4);
        product.addTiePointGrid(lon4);
        Band band4 = new VirtualBand("B4", ProductData.TYPE_INT16, 3, 10, "X");
        product.addBand(band4);
        band4.setGeoCoding(new TiePointGeoCoding(lat4, lon4));
        double oneThird = ((double) 2 / 3);
        for (double y = 0; y < 9; y++) {
            for (double x = 0; x < 3; x++) {
                final PixelPos band1Pos = new PixelPos(x, y);
                final PixelPos transformedBand1Pos = SceneRasterTransformUtils.transformFromToRasterDataNodeRaster(band4, band3, band1Pos);
                Assert.assertEquals(band1Pos.getX() * oneThird, transformedBand1Pos.getX(), 1e-8);
                Assert.assertEquals(band1Pos.getY() * 0.4, transformedBand1Pos.getY(), 1e-8);
            }
        }
        for (double y = 0; y < 4; y++) {
            for (double x = 0; x < 2; x++) {
                final PixelPos band2Pos = new PixelPos(x, y);
                final PixelPos transformedBand2Pos = SceneRasterTransformUtils.transformFromToRasterDataNodeRaster(band3, band4, band2Pos);
                Assert.assertEquals(band2Pos.getX() * 1.5, transformedBand2Pos.getX(), 1e-8);
                Assert.assertEquals(band2Pos.getY() * 2.5, transformedBand2Pos.getY(), 1e-8);
            }
        }
    }

    @Test
    public void testTransformToProductGrid_Shape() throws TransformException, SceneRasterTransformException {
//        Product product = new Product("A", "B", 4, 8);
//        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 4, 8, new float[]{1f, 5f, 1f, 5f});
//        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 4, 8, new float[]{1f, 1f, 9f, 9f});
//        product.addTiePointGrid(lat);
//        product.addTiePointGrid(lon);
//        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
//        TiePointGrid lat2 = new TiePointGrid("lat2", 2, 2, 0, 0, 8, 16, new float[]{1f, 5f, 1f, 5f});
//        TiePointGrid lon2 = new TiePointGrid("lon2", 2, 2, 0, 0, 8, 16, new float[]{1f, 1f, 9f, 9f});
//        product.addTiePointGrid(lat2);
//        product.addTiePointGrid(lon2);
//        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 4, 8, "X");
//        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 8, 16, "Y");
//        product.addBand(band1);
//        product.addBand(band2);
//        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        int[] xpoints = {2, 6, 6, 4};
        int[] ypoints = {2, 4, 12, 14};
        Polygon polygon = new Polygon(xpoints, ypoints, 4);
        Shape transformedShape = SceneRasterTransformUtils.transformToProductRaster(band2, polygon);
        Assert.assertEquals(true, transformedShape instanceof GeneralPath);
        PathIterator transformedShapePathIterator = transformedShape.getPathIterator(null);
        double[] currentSegment = new double[2];
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(1.0, currentSegment[0], 1e-8);
        Assert.assertEquals(1.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(3.0, currentSegment[0], 1e-8);
        Assert.assertEquals(2.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(3.0, currentSegment[0], 1e-8);
        Assert.assertEquals(6.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(2.0, currentSegment[0], 1e-8);
        Assert.assertEquals(7.0, currentSegment[1], 1e-8);
    }

    @Test
    public void testTransformToRasterGrid_Shape() throws TransformException, SceneRasterTransformException {
//        Product product = new Product("A", "B", 4, 8);
//        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 4, 8, new float[]{1f, 5f, 1f, 5f});
//        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 4, 8, new float[]{1f, 1f, 9f, 9f});
//        product.addTiePointGrid(lat);
//        product.addTiePointGrid(lon);
//        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
//        TiePointGrid lat2 = new TiePointGrid("lat2", 2, 2, 0, 0, 8, 16, new float[]{1f, 5f, 1f, 5f});
//        TiePointGrid lon2 = new TiePointGrid("lon2", 2, 2, 0, 0, 8, 16, new float[]{1f, 1f, 9f, 9f});
//        product.addTiePointGrid(lat2);
//        product.addTiePointGrid(lon2);
//        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 4, 8, "X");
//        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 8, 16, "Y");
//        product.addBand(band1);
//        product.addBand(band2);
//        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));
        int[] xpoints = {1, 3, 3, 2};
        int[] ypoints = {1, 2, 6, 7};
        Polygon polygon = new Polygon(xpoints, ypoints, 4);
        Shape transformedShape = SceneRasterTransformUtils.transformToRasterDataNodeRaster(band2, polygon);
        Assert.assertEquals(true, transformedShape instanceof GeneralPath);
        PathIterator transformedShapePathIterator = transformedShape.getPathIterator(null);
        double[] currentSegment = new double[2];
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(2.0, currentSegment[0], 1e-8);
        Assert.assertEquals(2.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(6.0, currentSegment[0], 1e-8);
        Assert.assertEquals(4.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(6.0, currentSegment[0], 1e-8);
        Assert.assertEquals(12.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(4.0, currentSegment[0], 1e-8);
        Assert.assertEquals(14.0, currentSegment[1], 1e-8);
    }

    @Test
    public void testTransformFromToRasterGrid_Shape() throws TransformException, SceneRasterTransformException {
        Product product = new Product("A", "B", 2, 4);
        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 2, 4, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 2, 4, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        TiePointGrid lat2 = new TiePointGrid("lat2", 2, 2, 0, 0, 4, 8, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon2 = new TiePointGrid("lon2", 2, 2, 0, 0, 4, 8, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat2);
        product.addTiePointGrid(lon2);
        TiePointGrid lat3 = new TiePointGrid("lat3", 2, 2, 0, 0, 6, 12, new float[]{1f, 5f, 1f, 5f});
        TiePointGrid lon3 = new TiePointGrid("lon3", 2, 2, 0, 0, 6, 12, new float[]{1f, 1f, 9f, 9f});
        product.addTiePointGrid(lat3);
        product.addTiePointGrid(lon3);
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 6, 12, "X");
        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 4, 8, "Y");
        product.addBand(band1);
        product.addBand(band2);
        band1.setGeoCoding(new TiePointGeoCoding(lat3, lon3));
        band2.setGeoCoding(new TiePointGeoCoding(lat2, lon2));

        //band1 -> band2
        int[] xpoints = {3, 5, 4, 2};
        int[] ypoints = {1, 9, 11, 3};
        Polygon polygon = new Polygon(xpoints, ypoints, 4);
        Shape transformedShape = SceneRasterTransformUtils.transformFromToRasterDataNodeRaster(band1, band2, polygon);
        Assert.assertEquals(true, transformedShape instanceof GeneralPath);
        PathIterator transformedShapePathIterator = transformedShape.getPathIterator(null);
        double oneThird = ((double) 1 / 3);
        double[] currentSegment = new double[2];
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(2.0, currentSegment[0], 1e-8);
        Assert.assertEquals(2 * oneThird, currentSegment[1], 1e-7);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(3.0 + oneThird, currentSegment[0], 1e-6);
        Assert.assertEquals(6.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(2.0 + (2 * oneThird), currentSegment[0], 1e-7);
        Assert.assertEquals(7.0 + oneThird, currentSegment[1], 1e-6);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(1.0 + oneThird, currentSegment[0], 1e-7);
        Assert.assertEquals(2.0, currentSegment[1], 1e-8);

        //band2 -> band1
        xpoints = new int[]{1, 3, 3, 2};
        ypoints = new int[]{2, 7, 6, 1};
        polygon = new Polygon(xpoints, ypoints, 4);
        transformedShape = SceneRasterTransformUtils.transformFromToRasterDataNodeRaster(band2, band1, polygon);
        Assert.assertEquals(true, transformedShape instanceof GeneralPath);
        transformedShapePathIterator = transformedShape.getPathIterator(null);
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(1.5, currentSegment[0], 1e-8);
        Assert.assertEquals(3.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(4.5, currentSegment[0], 1e-8);
        Assert.assertEquals(10.5, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(4.5, currentSegment[0], 1e-8);
        Assert.assertEquals(9.0, currentSegment[1], 1e-8);
        transformedShapePathIterator.next();
        transformedShapePathIterator.currentSegment(currentSegment);
        Assert.assertEquals(3.0, currentSegment[0], 1e-8);
        Assert.assertEquals(1.5, currentSegment[1], 1e-8);
    }


    @Test
    public void testTransformShapeToRasterCoordinates_Path2D_identity() throws Exception {
        final Shape path = getPathInProductCoordinates();
        double[][] expectedCoords = {{1.0, 1.0}, {1.0, 7.0}, {3.0, 7.0}, {3.0, 1.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToRasterCoordinates(
                path, band1.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof Path2D.Double);
        assertTransformedCorrectly((Path2D.Double) shapeInRasterCoordinates, expectedCoords);
    }

    @Test
    public void testTransformShapeToProductCoordinates_Path2D_identity() throws Exception {
        final Shape path = getPathInProductCoordinates();
        double[][] expectedCoords = {{1.0, 1.0}, {1.0, 7.0}, {3.0, 7.0}, {3.0, 1.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToProductCoordinates(
                path, band1.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof Path2D.Double);
        assertTransformedCorrectly((Path2D.Double) shapeInRasterCoordinates, expectedCoords);
    }

    @Test
    public void testTransformShapeToRasterCoordinates_Path2D() throws Exception {
        final Shape path = getPathInProductCoordinates();
        double[][] expectedCoords = {{2.0, 2.0}, {2.0, 14.0}, {6.0, 14.0}, {6.0, 2.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToRasterCoordinates(
                path, band2.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof Path2D.Double);
        assertTransformedCorrectly((Path2D.Double) shapeInRasterCoordinates, expectedCoords);
    }

    @Test
    public void testTransformShapeToProductCoordinates_Path2D() throws Exception {
        final Shape path = getPathInRasterCoordinates();
        double[][] expectedCoords = {{1.0, 1.0}, {1.0, 7.0}, {3.0, 7.0}, {3.0, 1.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToProductCoordinates(
                path, band2.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof Path2D.Double);
        assertTransformedCorrectly((Path2D.Double) shapeInRasterCoordinates, expectedCoords);
    }

    @Test
    public void testTransformShapeToRasterCoordinates_Rectangle() throws Exception {
        final Shape path = getPathInProductCoordinates();
        double[][] expectedCoords = {{2.0, 2.0}, {6.0, 2.0}, {6.0, 14.0}, {2.0, 14.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToRasterCoordinates(
                path.getBounds2D(), band2.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof GeneralPath);
        assertTransformedCorrectly((GeneralPath) shapeInRasterCoordinates, expectedCoords);
    }

    @Test
    public void testTransformShapeToProductCoordinates_Rectangle() throws Exception {
        final Shape path = getPathInRasterCoordinates();
        double[][] expectedCoords = {{1.0, 1.0}, {3.0, 1.0}, {3.0, 7.0}, {1.0, 7.0}};

        final Shape shapeInRasterCoordinates = SceneRasterTransformUtils.transformShapeToProductCoordinates(
                path.getBounds2D(), band2.getSceneRasterTransform());

        assert(shapeInRasterCoordinates instanceof GeneralPath);
        assertTransformedCorrectly((GeneralPath) shapeInRasterCoordinates, expectedCoords);
    }

    private void assertTransformedCorrectly(Path2D.Double transformed, double[][] expectedCoords) {
        final PathIterator pathIterator = transformed.getPathIterator(null);
        double[] coords = new double[6];
        int segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_MOVETO, segmentType);
        assertEquals(expectedCoords[0][0], coords[0], 1e-8);
        assertEquals(expectedCoords[0][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[1][0], coords[0], 1e-8);
        assertEquals(expectedCoords[1][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[2][0], coords[0], 1e-8);
        assertEquals(expectedCoords[2][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[3][0], coords[0], 1e-8);
        assertEquals(expectedCoords[3][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_CLOSE, segmentType);
    }

    private void assertTransformedCorrectly(GeneralPath transformed, double[][] expectedCoords) {
        final PathIterator pathIterator = transformed.getPathIterator(null);
        double[] coords = new double[6];
        int segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_MOVETO, segmentType);
        assertEquals(expectedCoords[0][0], coords[0], 1e-8);
        assertEquals(expectedCoords[0][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[1][0], coords[0], 1e-8);
        assertEquals(expectedCoords[1][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[2][0], coords[0], 1e-8);
        assertEquals(expectedCoords[2][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[3][0], coords[0], 1e-8);
        assertEquals(expectedCoords[3][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_LINETO, segmentType);
        assertEquals(expectedCoords[0][0], coords[0], 1e-8);
        assertEquals(expectedCoords[0][1], coords[1], 1e-8);
        pathIterator.next();
        segmentType = pathIterator.currentSegment(coords);
        assertEquals(PathIterator.SEG_CLOSE, segmentType);
    }

    private Shape getPathInProductCoordinates() {
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(1, 1);
        path.lineTo(1, 7);
        path.lineTo(3, 7);
        path.lineTo(3, 1);
        path.closePath();
        return path;
    }

    private Shape getPathInRasterCoordinates() {
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(2, 2);
        path.lineTo(2, 14);
        path.lineTo(6, 14);
        path.lineTo(6, 2);
        path.closePath();
        return path;
    }

}