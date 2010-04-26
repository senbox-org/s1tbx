package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;

import java.util.Arrays;

public class TileImplTest extends TestCase {
    private static final int W = 32;
    private static final int H = 16;

    public void testGetSamplesFloat() {
        Tile tile;
        float[] samples;

        tile = createRawTile(ProductData.TYPE_FLOAT32);
        assertNotNull(tile.getDataBufferFloat());
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertSame(samples, tile.getDataBufferFloat());
        assertEquals(W * H, samples.length);
        assertEquals(1.1F, samples[0], 1e-10F);
        assertEquals(2.1F, samples[1], 1e-10F);
        assertEquals(3.1F, samples[2], 1e-10F);
        assertEquals(512.1F, samples[W * H - 1], 1e-10F);

        tile = createRawTile(ProductData.TYPE_FLOAT64);
        assertNull(tile.getDataBufferFloat());
        assertNotNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(1.1F, samples[0], 1e-10F);
        assertEquals(2.1F, samples[1], 1e-10F);
        assertEquals(3.1F, samples[2], 1e-10F);
        assertEquals(512.1F, samples[W * H - 1], 1e-10F);

        tile = createScaledTile(ProductData.TYPE_UINT16, 2.5);
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(2.5F, samples[0], 1e-10F);
        assertEquals(5.0F, samples[1], 1e-10F);
        assertEquals(7.5F, samples[2], 1e-10F);
        assertEquals(1280.0F, samples[W * H - 1], 1e-10F);

        tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(2.5F, samples[0], 1e-10F);
        assertEquals(5.0F, samples[1], 1e-10F);
        assertEquals(true, Float.isNaN(samples[2])); // no-data = 7.5
        assertEquals(1280.0F, samples[W * H - 1], 1e-10F);
    }

    public void testGetSamplesDouble() {
        Tile tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);

        assertNull(tile.getDataBufferDouble());
        double[] samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(2.5, samples[0], 1e-10);
        assertEquals(5.0, samples[1], 1e-10);
        assertEquals(true, Double.isNaN(samples[2])); // no-data = 7.5
        assertEquals(1280.0, samples[W * H - 1], 1e-10);
    }

    public void testSetSamples() {
        Tile tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);

        double[] samples;
        float[] newSamplesF = new float[W * H];
        Arrays.fill(newSamplesF, 5.1F);
        tile.setSamples(newSamplesF);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(5.0F, samples[0], 1e-10F);
        assertEquals(5.0F, samples[W * H - 1], 1e-10F);

        double[] newSamplesD = new double[W * H];
        Arrays.fill(newSamplesD, 12.2);
        tile.setSamples(newSamplesD);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(W * H, samples.length);
        assertEquals(10.0, samples[0], 1e-10);
        assertEquals(10.0, samples[W * H - 1], 1e-10);
    }

    static Tile createRawTile(int type) {
        return createScaledTile(type, 1.0);
    }

    static Tile createScaledTile(int type, double scalingFactor) {
        return _createTile(type, scalingFactor, false, 0);
    }

    static Tile createScaledTileWithNaNs(int type, double scalingFactor, double noDataValue) {
        return _createTile(type, scalingFactor, true, noDataValue);
    }

    private static Tile _createTile(int type, double scalingFactor,
                                   boolean dataValueUsed, double noDataValue) {
        Product product = new Product("n", "t", W, H);
        Band band = product.addBand("x", type);
        band.setScalingFactor(scalingFactor);
        band.setGeophysicalNoDataValue(noDataValue);
        band.setNoDataValueUsed(dataValueUsed);

        ProductData rasterData = band.createCompatibleRasterData();
        for (int i = 0; i < W * H; i++) {
            rasterData.setElemDoubleAt(i, (i + 1) + 0.1);
        }
        band.setRasterData(rasterData);

        return new TileImpl(band, band.getSourceImage().getData());
    }

}
