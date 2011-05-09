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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.junit.Test;

import static org.junit.Assert.*;

public class StxTest {

    @Test
    public void testSignedByteBandStatistics() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_INT8, 11, 13);
        final Stx stx = Stx.create(band, 0, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 1.0e-1);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(41.4, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatistics() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 100, 120);
        final Stx stx = Stx.create(band, 0, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 0.0);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(3464.2, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatisticsWithGapsInHistogram() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 10, 12);
        Stx stx = Stx.create(band, 0, ProgressMonitor.NULL);
        assertEquals(0.0, stx.getMedian(), 0.0);
        assertEquals(0.0, stx.getMean(), 0.0);
        assertEquals(34.8, stx.getStandardDeviation(), 1.0e-1);
    }

    @Test
    public void testFloatBandStatisticsWithNoDataValueSet() throws Exception {
        final Band band = createTestBand(ProductData.TYPE_FLOAT32, 100, 120);
        band.setNoDataValueUsed(true);
        band.setNoDataValue(-0.5);

        Stx stx = Stx.create(band, band.getValidMaskImage(), ProgressMonitor.NULL);
        assertEquals(5.0e-1, stx.getMedian(), 0.1e-1);
        assertEquals(4.1e-5, stx.getMean(), 0.1e-5);
        assertEquals(3464.4, stx.getStandardDeviation(), 1.0e-1);

        band.setNoDataValue(0.5);
        stx = Stx.create(band, band.getValidMaskImage(), ProgressMonitor.NULL);
        assertEquals(-5.0e-1, stx.getMedian(), 0.1e-1);
        assertEquals(-4.1e-5, stx.getMean(), 0.1e-5);
        assertEquals(3464.4, stx.getStandardDeviation(), 1.0e-1);
    }

    private Band createTestBand(int type, int w, int h) {
        final Product product = new Product("F", "F", w, h);
        final double mean = (w * h - 1.0) / 2.0;
        final Band band = new VirtualBand("V", type, w, h, "(Y-0.5) * " + w + " + (X-0.5) - " + mean);
        product.addBand(band);

        return band;
    }
}
