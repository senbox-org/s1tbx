package org.esa.beam.binning;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ObservationIteratorTest {

    @Test
    public void testIteration() throws Exception {

        int width = 18;
        int height = 36;
        int size = width * height;
        Raster sourceTile = Raster.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, new Point(0, 0));
        WritableRaster maskTile = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, width, height, 1, new Point(0, 0));
        int[] maskData = new int[size];
        Arrays.fill(maskData, 1);
        maskTile.setPixels(0, 0, width, height, maskData);
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 10.0, 10.0);
        ObservationIterator iterator = new ObservationIterator(new Raster[]{sourceTile}, maskTile,
                                                               new float[]{0.5f}, gc);

        assertTrue(iterator.hasNext());
        Observation observation = iterator.next();
        assertNotNull(observation);
    }

    @Test
    public void testSampleCounter() throws Exception {
        Rectangle bounds = new Rectangle(0, 0, 2, 3);
        float[] superSamplingSteps = new float[]{0.5f};
        Point2D.Float center = new Point2D.Float(superSamplingSteps[0], superSamplingSteps[0]);
        ObservationIterator.SamplePointer pointer = new ObservationIterator.SamplePointer(bounds,
                                                                                          new Point2D.Float[]{center});

        assertTrue(pointer.canMove());
        pointer.move();

        assertEquals(0, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(center, pointer.getSuperSamplingPoint());

        assertTrue(pointer.canMove());
        pointer.move();

        assertEquals(1, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(center, pointer.getSuperSamplingPoint());

        assertTrue(pointer.canMove());
        pointer.move();

        assertEquals(0, pointer.getX());
        assertEquals(1, pointer.getY());

        movePointer(pointer, 3);

        assertEquals(1, pointer.getX());
        assertEquals(2, pointer.getY());

        assertFalse(pointer.canMove());
        try {
            pointer.move();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }

    }

    private void movePointer(ObservationIterator.SamplePointer pointer, int steps) {
        for (int i = 0; i < steps; i++) {
            pointer.move();
        }
    }

    @Test
    public void testSampleCounterWithSuperSampling() throws Exception {
        Rectangle bounds = new Rectangle(0, 0, 2, 3);
        Point2D.Float[] superSamplingPoints = {
                new Point2D.Float(0.33f, 0.33f),
                new Point2D.Float(0.66f, 0.66f),
                new Point2D.Float(0.99f, 0.99f),
        };
        ObservationIterator.SamplePointer pointer = new ObservationIterator.SamplePointer(bounds,
                                                                                          superSamplingPoints);

        assertTrue(pointer.canMove());
        pointer.move();

        assertEquals(0, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(superSamplingPoints[0], pointer.getSuperSamplingPoint());

        assertTrue(pointer.canMove());
        pointer.move();
        assertEquals(0, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(superSamplingPoints[1], pointer.getSuperSamplingPoint());

        assertTrue(pointer.canMove());
        pointer.move();
        assertEquals(0, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(superSamplingPoints[2], pointer.getSuperSamplingPoint());

        assertTrue(pointer.canMove());
        pointer.move();
        assertEquals(1, pointer.getX());
        assertEquals(0, pointer.getY());
        assertEquals(superSamplingPoints[0], pointer.getSuperSamplingPoint());

        movePointer(pointer, 13);

        assertTrue(pointer.canMove());
        assertEquals(1, pointer.getX());
        assertEquals(2, pointer.getY());
        assertEquals(superSamplingPoints[1], pointer.getSuperSamplingPoint());
    }

}