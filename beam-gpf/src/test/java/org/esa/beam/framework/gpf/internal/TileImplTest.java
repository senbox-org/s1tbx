/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;

import java.util.Arrays;

public class TileImplTest extends TestCase {

    private static final int W = 16;
    private static final int H = 8;
    private static final int N = W * H;
    private static final int N05 = N / 2;

    public void testSignedAndUnsignedByteScaledSamples() {
        Tile tile;
        float[] samples;

        tile = createIntTile(ProductData.TYPE_INT8, -N05, 0.1);
        samples = tile.getSamplesFloat();

        assertEquals(-N05 * 0.1F, samples[0], 1.0e-5F);
        assertEquals((-N05 + 1) * 0.1F, samples[1], 1.0e-5F);
        assertEquals(-1 * 0.1F, samples[N05 - 1], 1.0e-5F);
        assertEquals(0 * 0.1F, samples[N05], 1.0e-5F);
        assertEquals(1 * 0.1F, samples[N05 + 1], 1.0e-5F);
        assertEquals((N05 - 2) * 0.1F, samples[N - 2], 1.0e-5F);
        assertEquals((N05 - 1) * 0.1F, samples[N - 1], 1.0e-5F);

        assertEquals(-N05 * 0.1F, tile.getSampleFloat(0, 0), 1.0e-5F);
        assertEquals((-N05 + 1) * 0.1F, tile.getSampleFloat(1, 0), 1.0e-5F);
        assertEquals(-1 * 0.1F, tile.getSampleFloat(W - 1, H / 2 - 1), 1.0e-5F);
        assertEquals(0 * 0.1F, tile.getSampleFloat(0, H / 2), 1.0e-5F);
        assertEquals(1 * 0.1F, tile.getSampleFloat(1, H / 2), 1.0e-5F);
        assertEquals((N05 - 2) * 0.1F, tile.getSampleFloat(W - 2, H - 1), 1.0e-5F);
        assertEquals((N05 - 1) * 0.1F, tile.getSampleFloat(W - 1, H - 1), 1.0e-5F);

        tile = createIntTile(ProductData.TYPE_UINT8, 0, 0.1);
        samples = tile.getSamplesFloat();

        assertEquals(0 * 0.1F, samples[0], 1.0e-5F);
        assertEquals(1 * 0.1F, samples[1], 1.0e-5F);
        assertEquals((N05 - 1) * 0.1F, samples[N05 - 1], 1.0e-5F);
        assertEquals(N05 * 0.1F, samples[N05], 1.0e-5F);
        assertEquals((N05 + 1) * 0.1F, samples[N05 + 1], 1.0e-5F);
        assertEquals((N - 2) * 0.1F, samples[N - 2], 1.0e-5F);
        assertEquals((N - 1) * 0.1F, samples[N - 1], 1.0e-5F);

        assertEquals(0 * 0.1F, tile.getSampleFloat(0, 0), 1.0e-5F);
        assertEquals(1 * 0.1F, tile.getSampleFloat(1, 0), 1.0e-5F);
        assertEquals((N05 - 1) * 0.1F, tile.getSampleFloat(W - 1, H / 2 - 1), 1.0e-5F);
        assertEquals(N05 * 0.1F, tile.getSampleFloat(0, H / 2), 1.0e-5F);
        assertEquals((N05 + 1) * 0.1F, tile.getSampleFloat(1, H / 2), 1.0e-5F);
        assertEquals((N - 2) * 0.1F, tile.getSampleFloat(W - 2, H - 1), 1.0e-5F);
        assertEquals((N - 1) * 0.1F, tile.getSampleFloat(W - 1, H - 1), 1.0e-5F);
    }

    public void testSignedAndUnsignedByteUnscaledSamples() {
        Tile tile;
        int[] samples;

        tile = createIntTile(ProductData.TYPE_INT8, -N05, 1.0);
        samples = tile.getSamplesInt();
        assertEquals(N, samples.length);

        assertEquals(-N05, samples[0]);
        assertEquals(-N05 + 1, samples[1]);
        assertEquals(-1, samples[N05 - 1]);
        assertEquals(0, samples[N05]);
        assertEquals(1, samples[N05 + 1]);
        assertEquals(N05 - 2, samples[N - 2]);
        assertEquals(N05 - 1, samples[N - 1]);

        assertEquals(-N05, tile.getSampleInt(0, 0));
        assertEquals(-N05 + 1, tile.getSampleInt(1, 0));
        assertEquals(-1, tile.getSampleInt(W - 1, H / 2 - 1));
        assertEquals(0, tile.getSampleInt(0, H / 2));
        assertEquals(1, tile.getSampleInt(1, H / 2));
        assertEquals(N05 - 2, tile.getSampleInt(W - 2, H - 1));
        assertEquals(N05 - 1, tile.getSampleInt(W - 1, H - 1));

        tile = createIntTile(ProductData.TYPE_UINT8, 0, 1.0);
        samples = tile.getSamplesInt();

        assertEquals(0, samples[0]);
        assertEquals(1, samples[1]);
        assertEquals((N05 - 1), samples[N05 - 1]);
        assertEquals(N05, samples[N05]);
        assertEquals((N05 + 1), samples[N05 + 1]);
        assertEquals(N - 2, samples[N - 2]);
        assertEquals(N - 1, samples[N - 1]);

        assertEquals(0, tile.getSampleInt(0, 0));
        assertEquals(1, tile.getSampleInt(1, 0));
        assertEquals((N05 - 1), tile.getSampleInt(W - 1, H / 2 - 1));
        assertEquals(N05, tile.getSampleInt(0, H / 2));
        assertEquals((N05 + 1), tile.getSampleInt(1, H / 2));
        assertEquals(N - 2, tile.getSampleInt(W - 2, H - 1));
        assertEquals(N - 1, tile.getSampleInt(W - 1, H - 1));

    }

    public void testSetSamplePreventsOverflow() {
        Product product = new Product("n", "t", 1, 1);
        Band band = product.addBand("x", ProductData.TYPE_INT8);
        band.setRasterData(band.createCompatibleRasterData());
        double scalingFactor = 2.5;
        band.setScalingFactor(scalingFactor);

        Tile scaledTile = new TileImpl(band, band.getSourceImage().getData());

        int maxRawValue = Byte.MAX_VALUE;
        double geoPhysicalValueOutOfRawRange = (maxRawValue + 1) * scalingFactor;
        scaledTile.setSample(0, 0, geoPhysicalValueOutOfRawRange);
        assertEquals(maxRawValue * scalingFactor, scaledTile.getSampleDouble(0, 0), 1.0e-6);

        int minRawValue = Byte.MIN_VALUE;
        geoPhysicalValueOutOfRawRange = (minRawValue - 1) * scalingFactor;
        scaledTile.setSample(0, 0, geoPhysicalValueOutOfRawRange);
        assertEquals(minRawValue * scalingFactor, scaledTile.getSampleDouble(0, 0), 1.0e-6);
    }

    public void testGetSamplesFloat() {
        Tile tile;
        float[] samples;

        tile = createRawTile(ProductData.TYPE_FLOAT32);
        assertNotNull(tile.getDataBufferFloat());
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertSame(samples, tile.getDataBufferFloat());
        assertEquals(N, samples.length);
        assertEquals(1.1F, samples[0], 1.0e-5F);
        assertEquals(2.1F, samples[1], 1.0e-5F);
        assertEquals(3.1F, samples[2], 1.0e-5F);
        assertEquals(128.1F, samples[N - 1], 1.0e-5F);

        tile = createRawTile(ProductData.TYPE_FLOAT64);
        assertNull(tile.getDataBufferFloat());
        assertNotNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(1.1F, samples[0], 1.0e-5F);
        assertEquals(2.1F, samples[1], 1.0e-5F);
        assertEquals(3.1F, samples[2], 1.0e-5F);
        assertEquals(128.1F, samples[N - 1], 1.0e-5F);

        tile = createScaledTile(ProductData.TYPE_UINT16, 2.5);
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(2.5F, samples[0], 1.0e-5F);
        assertEquals(5.0F, samples[1], 1.0e-5F);
        assertEquals(7.5F, samples[2], 1.0e-5F);
        assertEquals(320.0F, samples[N - 1], 1.0e-5F);

        tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);
        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesFloat();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(2.5F, samples[0], 1.0e-5F);
        assertEquals(5.0F, samples[1], 1.0e-5F);
        assertEquals(true, Float.isNaN(samples[2])); // no-data = 7.5
        assertEquals(320.0F, samples[N - 1], 1.0e-5F);
    }

    public void testGetSamplesDouble() {
        Tile tile;
        double[] samples;

        tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);

        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(2.5, samples[0], 1.0e-10);
        assertEquals(5.0, samples[1], 1.0e-10);
        assertEquals(true, Double.isNaN(samples[2])); // no-data = 7.5
        assertEquals(320.0, samples[N - 1], 1.0e-10);

        tile = createRawTile(ProductData.TYPE_FLOAT32);

        assertNull(tile.getDataBufferDouble());
        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(1.1, samples[0], 1.0e-5F);
        assertEquals(2.1, samples[1], 1.0e-5F);
        assertEquals(128.1F, samples[N - 1], 1.0e-5F);
    }

    public void testSetSamples() {
        Tile tile = createScaledTileWithNaNs(ProductData.TYPE_UINT16, 2.5, 7.5);

        double[] samples;
        float[] newSamplesF = new float[N];
        Arrays.fill(newSamplesF, 5.1F);
        tile.setSamples(newSamplesF);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(5.0F, samples[0], 1.0e-5F);
        assertEquals(5.0F, samples[N - 1], 1.0e-5F);

        double[] newSamplesD = new double[N];
        Arrays.fill(newSamplesD, 12.2);
        tile.setSamples(newSamplesD);

        samples = tile.getSamplesDouble();
        assertNotNull(samples);
        assertEquals(N, samples.length);
        assertEquals(10.0, samples[0], 1.0e-10);
        assertEquals(10.0, samples[N - 1], 1.0e-10);
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
        for (int i = 0; i < N; i++) {
            rasterData.setElemDoubleAt(i, (i + 1) + 0.1);
        }
        band.setRasterData(rasterData);

        return new TileImpl(band, band.getSourceImage().getData());
    }

    private static Tile createIntTile(int type, int i0, double scalingFactor) {
        Product product = new Product("n", "t", W, H);
        Band band = product.addBand("x", type);
        band.setScalingFactor(scalingFactor);

        ProductData rasterData = band.createCompatibleRasterData();
        for (int i = 0; i < N; i++) {
            rasterData.setElemDoubleAt(i, i0 + i);
        }
        band.setRasterData(rasterData);

        return new TileImpl(band, band.getSourceImage().getData());
    }
}
