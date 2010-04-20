package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class TileImplTest extends TestCase {

    public void testGetSamplesFloat() {
        Tile tile = createTile(0, 0, 32, 16);

        assertNull(tile.getDataBufferDouble());
        float[] samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(32 * 16, samples.length);
        assertEquals(2.5F, samples[0], 1e-10F);
        assertEquals(5.0F, samples[1], 1e-10F);
        assertEquals(1280.0F, samples[32 * 16 - 1], 1e-10F);
    }

    public void testGetSamplesDouble() {
        Tile tile = createTile(0, 0, 32, 16);

        assertNull(tile.getDataBufferDouble());
        double[] samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(32 * 16, samples.length);
        assertEquals(2.5, samples[0], 1e-10);
        assertEquals(5.0, samples[1], 1e-10);
        assertEquals(1280.0, samples[32 * 16 - 1], 1e-10);
    }

    public void testSetSamples() {
        Tile tile = createTile(0, 0, 32, 16);

        double[] samples;
        float[] newSamplesF = new float[32 * 16];
        Arrays.fill(newSamplesF, 5.1F);
        tile.setSamples(newSamplesF);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(32 * 16, samples.length);
        assertEquals(5.0F, samples[0], 1e-10F);
        assertEquals(5.0F, samples[32 * 16 - 1], 1e-10F);

        double[] newSamplesD = new double[32 * 16];
        Arrays.fill(newSamplesD, 12.2);
        tile.setSamples(newSamplesD);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(32 * 16, samples.length);
        assertEquals(10.0, samples[0], 1e-10);
        assertEquals(10.0, samples[32 * 16 - 1], 1e-10);
    }

    static Tile createTile(int x0, int y0, int w, int h) {
        Band band = new Band("x", ProductData.TYPE_UINT16, w, h);
        band.setScalingFactor(2.5);
        WritableRaster raster =
                WritableRaster.createBandedRaster(DataBuffer.TYPE_USHORT, w, h, 1, new Point(x0, y0));
        final short[] data = ((DataBufferUShort) raster.getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = (short) (i + 1);
        }

        return new TileImpl(band, raster);
    }

}
