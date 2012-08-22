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

package org.esa.beam.statistics;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.StxFactory;
import org.junit.Test;

import javax.media.jai.Histogram;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class PrecisePercentileTest {

    private static final int HISTOGRAM_BIN_COUNT = 165;

    @Test
    public void testPrecisePercentile() throws Exception {
        final HashSet<Integer> peaks = new HashSet<Integer>();
        peaks.add(10);
        peaks.add(80);
        final Band raster = createBand(createBandData(1000, false, 0, peaks));
        final Histogram impreciseHistogram = new StxFactory()
                .withHistogramBinCount(HISTOGRAM_BIN_COUNT)
                .create(raster, ProgressMonitor.NULL)
                .getHistogram();

        final double imprecise5thPercentile = impreciseHistogram.getPTileThreshold(0.05)[0];
        PrecisePercentile precisePercentile = PrecisePercentile.createPrecisePercentile(raster, impreciseHistogram, 0.05);
        final double morePrecise5thPercentile = precisePercentile.percentile;
        final double maxErrorOn5thPercentile = precisePercentile.maxError;

        final double imprecise30thPercentile = impreciseHistogram.getPTileThreshold(0.3)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(raster, impreciseHistogram, 0.3);
        final double morePrecise30thPercentile = precisePercentile.percentile;
        final double maxErrorOn30thPercentile = precisePercentile.maxError;

        final double imprecise50thPercentile = impreciseHistogram.getPTileThreshold(0.5)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(raster, impreciseHistogram, 0.5);
        final double morePrecise50thPercentile = precisePercentile.percentile;
        final double maxErrorOn50thPercentile = precisePercentile.maxError;

        final double imprecise65thPercentile = impreciseHistogram.getPTileThreshold(0.65)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(raster, impreciseHistogram, 0.65);
        final double morePrecise65thPercentile = precisePercentile.percentile;
        final double maxErrorOn65thPercentile = precisePercentile.maxError;

        final double imprecise90thPercentile = impreciseHistogram.getPTileThreshold(0.9)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(raster, impreciseHistogram, 0.9);
        final double morePrecise90thPercentile = precisePercentile.percentile;
        final double maxErrorOn90thPercentile = precisePercentile.maxError;

        assertEquals(morePrecise5thPercentile, getValueCloserTo(morePrecise5thPercentile, imprecise5thPercentile, 10), 1E-8);
        assertEquals(morePrecise30thPercentile, getValueCloserTo(morePrecise30thPercentile, imprecise30thPercentile, 200), 1E-8);
        assertEquals(morePrecise50thPercentile, getValueCloserTo(morePrecise50thPercentile, imprecise50thPercentile, 600), 1E-8);
        assertEquals(morePrecise65thPercentile, getValueCloserTo(morePrecise65thPercentile, imprecise65thPercentile, 900), 1E-8);
        assertEquals(morePrecise90thPercentile, getValueCloserTo(morePrecise90thPercentile, imprecise90thPercentile, 1400), 1E-8);

        assertTrue(Math.abs(morePrecise5thPercentile - 10) <= maxErrorOn5thPercentile);
        assertTrue(Math.abs(morePrecise30thPercentile - 200) <= maxErrorOn30thPercentile);
        assertTrue(Math.abs(morePrecise50thPercentile - 600) <= maxErrorOn50thPercentile);
        assertTrue(Math.abs(morePrecise65thPercentile - 900) <= maxErrorOn65thPercentile);
        assertTrue(Math.abs(morePrecise90thPercentile - 1400) <= maxErrorOn90thPercentile);
    }

    @Test
    public void testPrecisePercentileForMultipleRasters() throws Exception {
        final HashSet<Integer> peaks = new HashSet<Integer>();
        peaks.add(10);
        peaks.add(80);
        final Band raster1 = createBand(createBandData(300, false, 0, peaks));
        final Band raster2 = createBand(createBandData(400, false, 0, peaks));
        final Histogram impreciseHistogram = new StxFactory()
                .withHistogramBinCount(30)
                .create(ProgressMonitor.NULL, null, new RasterDataNode[]{raster1, raster2})
                .getHistogram();

        final double imprecise5thPercentile = impreciseHistogram.getPTileThreshold(0.05)[0];
        PrecisePercentile precisePercentile = PrecisePercentile.createPrecisePercentile(new RasterDataNode[]{raster1, raster2}, impreciseHistogram, 0.05);
        final double morePrecise5thPercentile = precisePercentile.percentile;
        final double maxErrorOn5thPercentile = precisePercentile.maxError;

        final double imprecise30thPercentile = impreciseHistogram.getPTileThreshold(0.3)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(new RasterDataNode[]{raster1, raster2}, impreciseHistogram, 0.3);
        final double morePrecise30thPercentile = precisePercentile.percentile;
        final double maxErrorOn30thPercentile = precisePercentile.maxError;

        final double imprecise50thPercentile = impreciseHistogram.getPTileThreshold(0.5)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(new RasterDataNode[]{raster1, raster2}, impreciseHistogram, 0.5);
        final double morePrecise50thPercentile = precisePercentile.percentile;
        final double maxErrorOn50thPercentile = precisePercentile.maxError;

        final double imprecise65thPercentile = impreciseHistogram.getPTileThreshold(0.65)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(new RasterDataNode[]{raster1, raster2}, impreciseHistogram, 0.65);
        final double morePrecise65thPercentile = precisePercentile.percentile;
        final double maxErrorOn65thPercentile = precisePercentile.maxError;

        final double imprecise90thPercentile = impreciseHistogram.getPTileThreshold(0.9)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(new RasterDataNode[]{raster1, raster2}, impreciseHistogram, 0.9);
        final double morePrecise90thPercentile = precisePercentile.percentile;
        final double maxErrorOn90thPercentile = precisePercentile.maxError;

        assertEquals(morePrecise5thPercentile,  getValueCloserTo(morePrecise5thPercentile, imprecise5thPercentile,   10), 1E-8);
        assertEquals(morePrecise30thPercentile, getValueCloserTo(morePrecise30thPercentile, imprecise30thPercentile, 10), 1E-8);
        assertEquals(morePrecise50thPercentile, getValueCloserTo(morePrecise50thPercentile, imprecise50thPercentile, 80), 1E-8);
        assertEquals(morePrecise65thPercentile, getValueCloserTo(morePrecise65thPercentile, imprecise65thPercentile, 80), 1E-8);
        assertEquals(morePrecise90thPercentile, getValueCloserTo(morePrecise90thPercentile, imprecise90thPercentile, 260), 1E-8);

        assertTrue(Math.abs(morePrecise5thPercentile - 10) <= maxErrorOn5thPercentile + 1E-5);
        assertTrue(Math.abs(morePrecise30thPercentile - 10) <= maxErrorOn30thPercentile + 1E-5);
        assertTrue(Math.abs(morePrecise50thPercentile - 80) <= maxErrorOn50thPercentile + 1E-5);
        assertTrue(Math.abs(morePrecise65thPercentile - 80) <= maxErrorOn65thPercentile + 1E-5);
        assertTrue(Math.abs(morePrecise90thPercentile - 260) <= maxErrorOn90thPercentile + 1E-5);
    }

    private Band createBand(float[] bandData) {
        Product product = new Product("product", "type", bandData.length, 1);
        final Band band = product.addBand("name", ProductData.TYPE_FLOAT32);
        band.setData(ProductData.createInstance(bandData));
        return band;
    }

    private float[] createBandData(int dataCount, boolean dump, int startValue, Set<Integer> peaks) {
        float[] result = new float[dataCount];
        int i = startValue;
        int resultIdx = 0;
        boolean write = true;
        while (resultIdx < dataCount) {
            if (peaks.contains(i)) {
                for (int j = 0; j < 100; j++) {
                    result[resultIdx++] = i;
                    if (dump) System.out.println(i);
                }
            } else {
                if (write) {
                    result[resultIdx++] = i;
                    if (dump) System.out.println(i);
                    write = false;
                } else {
                    write = true;
                }
            }
            i++;
        }
        return result;
    }

    @Test
    public void testGetMorePrecisePercentile() throws Exception {
        final Band band = createBand(new float[]{
                0.0f, 1.5f,
                2.5f, 2.5f, 2.5f, 2.5f, 2.5f,
                2.5f, 2.5f, 2.5f, 2.5f, 2.5f,
                3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 10.5f, 11.5f, 12.5f,
                13.5f, 13.5f, 13.5f, 13.5f, 13.5f,
                13.5f, 13.5f, 13.5f, 13.5f, 13.5f,
                14.5f, 15.5f
        });
        final Histogram histogram = new StxFactory()
                .withHistogramBinCount(10)
                .create(band, ProgressMonitor.NULL)
                .getHistogram();

        final double imprecise30thPercentile = histogram.getPTileThreshold(0.3)[0];
        PrecisePercentile precisePercentile = PrecisePercentile.createPrecisePercentile(band, histogram, 0.3);
        final double morePrecise30thPercentile = precisePercentile.percentile;
        assertEquals(morePrecise30thPercentile, getValueCloserTo(imprecise30thPercentile, morePrecise30thPercentile, 2.5), 1E-8);

        final double imprecise50thPercentile = histogram.getPTileThreshold(0.5)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(band, histogram, 0.5);
        final double morePrecise50thPercentile = precisePercentile.percentile;
        assertEquals(morePrecise50thPercentile, getValueCloserTo(imprecise50thPercentile, morePrecise50thPercentile, 7.5), 1E-8);

        final double imprecise90thPercentile = histogram.getPTileThreshold(0.9)[0];
        precisePercentile = PrecisePercentile.createPrecisePercentile(band, histogram, 0.9);
        final double morePrecise90thPercentile = precisePercentile.percentile;
        assertEquals(morePrecise90thPercentile, getValueCloserTo(imprecise90thPercentile, morePrecise90thPercentile, 13.5), 1E-8);
    }

    private static double getValueCloserTo(double a, double b, double value) {
        return Math.abs(a - value) < Math.abs(b - value) ? a : b;
    }
}
