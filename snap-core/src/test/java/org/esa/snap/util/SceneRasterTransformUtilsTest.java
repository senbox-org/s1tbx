package org.esa.snap.util;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() {
        Product product = new Product("A", "B", 4, 8);
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