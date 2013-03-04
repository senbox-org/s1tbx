package org.esa.beam.binning;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class ObservationIteratorTest {

    @Test
    public void testIteration() throws Exception {

        int width = 18;
        int height = 36;
        Raster sourceTile = Raster.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, new Point(0, 0));
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 10.0, 10.0);
        Product product = new Product("name", "desc", width, height);
        product.setGeoCoding(gc);
        ObservationIterator iterator = ObservationIterator.create(new Raster[]{sourceTile}, product,
                                                                  null, new float[]{0.5f});

        assertTrue(iterator.hasNext());
        Observation observation = iterator.next();
        assertNotNull(observation);
        assertEquals(1, observation.size());
    }

    @Test
    public void testValuesOfIteratedSamples() throws Exception {

        int width = 12;
        int height = 10;
        Raster[] sourceRasters = createSourceRasters(width, height);
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 10.0, 10.0);
        Product product = new Product("name", "desc", width, height);
        product.setGeoCoding(gc);
        ObservationIterator iterator = ObservationIterator.create(sourceRasters, product,
                                                                  null, new float[]{0.5f});

        assertTrue(iterator.hasNext());
        Observation observation = iterator.next();
        assertNotNull(observation);
        assertEquals(1, observation.get(0), 1.0e-6);
        observation = iterate(iterator, 16);
        assertEquals(17, observation.get(0), 1.0e-6);
        observation = iterate(iterator, 45);
        assertEquals(62, observation.get(0), 1.0e-6);
        observation = iterate(iterator, 57);
        assertEquals(119, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(120, observation.get(0), 1.0e-6);
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException expected) {

        }
    }

    @Test
    public void testIterationWithMask() throws Exception {

        int width = 12;
        int height = 10;
        int size = width * height;
        Raster[] sourceRasters = createSourceRasters(width, height);

        WritableRaster maskTile = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, width, height, 1, new Point(0, 0));
        int[] maskData = new int[size];
        for (int i = 0; i < maskData.length; i++) {
            maskData[i] = i % 2;
        }
        maskTile.setPixels(0, 0, width, height, maskData);
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 10.0, 10.0);
        Product product = new Product("name", "desc", width, height);
        product.setGeoCoding(gc);
        ObservationIterator iterator = ObservationIterator.create(sourceRasters, product,
                                                                  maskTile, new float[]{0.5f});

        assertTrue(iterator.hasNext());
        Observation observation = iterator.next();
        assertNotNull(observation);
        assertEquals(2, observation.get(0), 1.0e-6);
        observation = iterate(iterator, 16);
        assertEquals(34, observation.get(0), 1.0e-6);

        observation = iterate(iterator, 43);
        assertEquals(120, observation.get(0), 1.0e-6);

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException expected) {
        }
    }

    @Test
    public void testSuperSampling() throws Exception {

        int width = 2;
        int height = 3;
        Raster[] sourceRasters = createSourceRasters(width, height);
        CrsGeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180, 90, 10.0, 10.0);
        Product product = new Product("name", "desc", width, height);
        product.setGeoCoding(gc);
        ObservationIterator iterator = ObservationIterator.create(sourceRasters, product,
                                                                  null, new float[]{0.25f, 0.75f});

        Observation observation = iterate(iterator, 16);
        assertEquals(4, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(5, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(5, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(5, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(5, observation.get(0), 1.0e-6);
        observation = iterator.next();
        assertEquals(6, observation.get(0), 1.0e-6);
        iterate(iterator, 3);
        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException expected) {
        }
    }

    private Raster[] createSourceRasters(int width, int height) {
        WritableRaster sourceTile = Raster.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, new Point(0, 0));
        int[] sourceData = new int[width * height];
        for (int i = 0; i < sourceData.length; i++) {
            sourceData[i] = i + 1;
        }
        sourceTile.setPixels(0, 0, width, height, sourceData);
        return new Raster[]{sourceTile};
    }

    private Observation iterate(ObservationIterator iterator, int steps) {
        Observation last = null;
        for (int i = 0; i < steps; i++) {
            last = iterator.next();
        }
        return last;
    }

}