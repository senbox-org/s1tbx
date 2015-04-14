package org.esa.snap.binning;

import org.junit.Test;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static org.junit.Assert.*;

public class SamplePointerTest {


    @Test
    public void testCreationOfSamplingPoints() throws Exception {
        Point2D.Float[] points = SamplePointer.createSamplingPoints(new float[]{1.0f / 6.0f, 3.0f / 6.0f, 5.0f / 6.0f});
        assertEquals(9, points.length);
        assertEquals(new Point2D.Float(1.0f / 6.0f, 1.0f / 6.0f), points[0]);
        assertEquals(new Point2D.Float(3.0f / 6.0f, 1.0f / 6.0f), points[1]);
        assertEquals(new Point2D.Float(5.0f / 6.0f, 1.0f / 6.0f), points[2]);
        assertEquals(new Point2D.Float(1.0f / 6.0f, 3.0f / 6.0f), points[3]);
        assertEquals(new Point2D.Float(3.0f / 6.0f, 3.0f / 6.0f), points[4]);
        assertEquals(new Point2D.Float(5.0f / 6.0f, 3.0f / 6.0f), points[5]);
        assertEquals(new Point2D.Float(1.0f / 6.0f, 5.0f / 6.0f), points[6]);
        assertEquals(new Point2D.Float(3.0f / 6.0f, 5.0f / 6.0f), points[7]);
        assertEquals(new Point2D.Float(5.0f / 6.0f, 5.0f / 6.0f), points[8]);
    }

    @Test
    public void testSamplePointerAllValid() throws Exception {
        int width = 2;
        int height = 3;
        PlanarImage[] sourceImages = createSourceImages(width, height);
        Rectangle[] bounds = {new Rectangle(0, 0, width, height)};
        SamplePointer pointer = SamplePointer.create(sourceImages, bounds);

        Point2D.Float center = new Point2D.Float(0.5f, 0.5f);

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

        movePointer(pointer, 3);

        assertEquals(0, pointer.getX());
        assertEquals(2, pointer.getY());

        pointer.move();

        assertEquals(1, pointer.getX());
        assertEquals(2, pointer.getY());

        assertFalse(pointer.canMove());
        try {
            pointer.move();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testSamplePointerAllValidWithSuperSampling() throws Exception {
        int width = 2;
        int height = 3;
        Rectangle[] bounds = {new Rectangle(0, 0, width, height)};
        PlanarImage[] sourceImages = createSourceImages(width, height);

        Point2D.Float[] superSamplingPoints = {
                new Point2D.Float(0.33f, 0.33f),
                new Point2D.Float(0.66f, 0.66f),
                new Point2D.Float(0.99f, 0.99f),
        };
        SamplePointer pointer = SamplePointer.create(sourceImages, bounds, superSamplingPoints);

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

    private PlanarImage[] createSourceImages(int width, int height) {
        WritableRaster sourceTile = Raster.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, new Point(0, 0));
        ColorModel cm = PlanarImage.getDefaultColorModel(sourceTile.getDataBuffer().getDataType(), 1);
        BufferedImage bufferedImage = new BufferedImage(cm, sourceTile, false, null);
        return new PlanarImage[]{PlanarImage.wrapRenderedImage(bufferedImage)};
    }

    private void movePointer(SamplePointer pointer, int steps) {
        for (int i = 0; i < steps; i++) {
            pointer.move();
        }
    }

}
