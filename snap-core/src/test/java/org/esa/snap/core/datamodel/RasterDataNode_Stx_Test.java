/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import static org.junit.Assert.*;

public class RasterDataNode_Stx_Test {

    @Test
    public void testValues_1_2_3_4_5_6() {
        final int w = 3;
        final int h = 2;
        final float[] floats = {1, 2, 3, 4, 5, 6};
        final Double noDataValue = null;
        final Band band = createBand("name", ProductData.TYPE_FLOAT32, w, h, floats, noDataValue);
        final Product product = new Product("p", "t", w, h);
        product.addBand(band);

        final Stx stx = band.getStx();
        assertNotNull(stx);
        assertEquals(0, stx.getResolutionLevel());
        assertEquals(1, stx.getMinimum(), 1e-11);
        assertEquals(6, stx.getMaximum(), 1e-11);
        assertEquals(3.5, stx.getMean(), 1e-11);
        assertEquals(1.8708286933869707, stx.getStandardDeviation(), 1e-11);
        assertEquals(0.009765625, stx.getHistogramBinWidth(), 1e-11);
        assertEquals(512, stx.getHistogramBinCount());

        final double[] values = {1, 2, 3, 4, 5, 6};
        final int[] binIdx = {0, 102, 204, 307, 409, 511};
        assertBinValues(stx, binIdx, values);
    }

    @Test
    public void testValues_1_2_3_4_5_6_NoDataValue_3() {
        final int w = 3;
        final int h = 2;
        final float[] floats = {1, 2, 3, 4, 5, 6};
        final Double noDataValue = 3d;
        final Band band = createBand("name", ProductData.TYPE_FLOAT32, w, h, floats, noDataValue);
        final Product product = new Product("p", "t", w, h);
        product.addBand(band);

        final Stx stx = band.getStx();
        assertNotNull(stx);
        assertEquals(0, stx.getResolutionLevel());
        assertEquals(1, stx.getMinimum(), 1e-11);
        assertEquals(6, stx.getMaximum(), 1e-11);
        assertEquals(3.6, stx.getMean(), 1e-11);
        assertEquals(2.073644135332772, stx.getStandardDeviation(), 1e-11);
        assertEquals(0.009765625, stx.getHistogramBinWidth(), 1e-11);
        assertEquals(512, stx.getHistogramBinCount());

        final double[] values = {1, 2, 4, 5, 6};
        final int[] binIdx = {0, 102, 307, 409, 511};
        assertBinValues(stx, binIdx, values);
    }

    @Test
    public void testValues_41_52_63_74_85_96() {
        final int w = 2;
        final int h = 3;
        final float[] floats = {4.1f, 5.2f, 6.3f, 7.4f, 8.5f, 9.6f};
        final Double noDataValue = null;
        final Band band = createBand("name", ProductData.TYPE_FLOAT32, w, h, floats, noDataValue);
        final Product product = new Product("p", "t", w, h);
        product.addBand(band);

        final Stx stx = band.getStx();
        assertNotNull(stx);
        assertEquals(0, stx.getResolutionLevel());
        assertEquals(4.1, stx.getMinimum(), 1e-6);
        assertEquals(9.6, stx.getMaximum(), 1e-6);
        assertEquals(6.850000063578288, stx.getMean(), 1e-11);
        assertEquals(2.057911715653782, stx.getStandardDeviation(), 1e-11);
        assertEquals(0.010742188431322575, stx.getHistogramBinWidth(), 1e-11);
        assertEquals(512, stx.getHistogramBinCount());

        final double[] values = {4.1, 5.2, 6.3, 7.4, 8.5, 9.6};
        final int[] binIdx = {0, 102, 204, 307, 409, 511};
        assertBinValues(stx, binIdx, values);
    }

    @Test
    public void testValues_41_52_63_74_85_96_NoDataValue_74() {
        final int w = 2;
        final int h = 3;
        final float[] floats = {4.1f, 5.2f, 6.3f, 7.4f, 8.5f, 9.6f};
        final Double noDataValue = 7.4d;
        final Band band = createBand("name", ProductData.TYPE_FLOAT32, w, h, floats, noDataValue);
        final Product product = new Product("p", "t", w, h);
        product.addBand(band);

        final Stx stx = band.getStx();
        assertNotNull(stx);
        assertEquals(0, stx.getResolutionLevel());
        assertEquals(4.1, stx.getMinimum(), 1e-6);
        assertEquals(9.6, stx.getMaximum(), 1e-6);
        assertEquals(6.740000057220459, stx.getMean(), 1e-11);
        assertEquals(2.281008719030014, stx.getStandardDeviation(), 1e-11);
        assertEquals(0.010742188431322575, stx.getHistogramBinWidth(), 1e-11);
        assertEquals(512, stx.getHistogramBinCount());

        final double[] values = {4.1, 5.2, 6.3, 8.5, 9.6};
        final int[] binIdx = {0, 102, 204, 409, 511};
        assertBinValues(stx, binIdx, values);
    }

    private void assertBinValues(Stx stx, int[] binIdx, double[] values) {
        assertEquals(binIdx.length, values.length);
        assertEquals(binIdx.length, stx.getSampleCount());
        final int[] bins = stx.getHistogramBins();
        final double binWidth = stx.getHistogramBinWidth();

        for (int i = 0; i < binIdx.length; i++) {
            assertEquals(1, bins[binIdx[i]]);
            final double min = stx.getHistogramBinMinimum(binIdx[i]);
            final double max = stx.getHistogramBinMaximum(binIdx[i]);
            assertEquals(true, min <= values[i] && min >= values[i] - binWidth);
            assertEquals(true, max >= values[i] && max <= values[i] + binWidth);
        }
    }

    private Band createBand(String name, int dataType, int w, int h, float[] floats, Double noDataValue) {
        final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 1, w, new int[]{0});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final TiledImage sourceImage = new TiledImage(0, 0, w, h, 0, 0, sm, cm);
        sourceImage.setData(WritableRaster.createWritableRaster(sm, new DataBufferFloat(floats, w * h), null));

        final Band band = new Band(name, dataType, w, h);
        band.setSourceImage(sourceImage);
        if (noDataValue != null) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(noDataValue);
        }
        return band;
    }
}
