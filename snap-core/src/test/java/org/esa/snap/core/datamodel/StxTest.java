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

import javax.media.jai.Histogram;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class StxTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMinIsNan() throws Exception {
        new Stx(Double.NaN, 1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMaxIsNan() throws Exception {
        new Stx(0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorResolutionLevelIsInvalid() throws Exception {
        new Stx(0.0, 1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), -2);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorHistogramIsNull() throws Exception {
        new Stx(0.0, 1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, (Histogram) null, 0);
    }

    @Test
    public void testConstructor() throws Exception {
        final Histogram histogram = new Histogram(256, -1, 1, 1);

        final Stx stx = new Stx(-1.0, 1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, histogram, 0);

        assertEquals(-1.0, stx.getMinimum(), 1E-10);
        assertEquals(1.0, stx.getMaximum(), 1E-10);
        assertEquals(Double.NaN, stx.getMean(), 1E-10);
        assertEquals(Double.NaN, stx.getStandardDeviation(), 1E-10);
        assertEquals(Double.NaN, stx.getMean(), 1E-10);
        assertEquals(Double.NaN, stx.getMedian(), 1E-10);
        assertEquals(false, stx.isIntHistogram());
        assertEquals(false, stx.isLogHistogram());
        assertSame(histogram, stx.getHistogram());
        assertSame(0, stx.getResolutionLevel());
    }

    @Test
    public void testPercentiles() throws Exception {
        int[] samples = new int[]{0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9, 9};

        Arrays.sort(samples);

        double p0 = samples[0];
        double p25 = samples[samples.length / 4 - 1];
        double p50 = samples[samples.length / 2 - 1];
        double p75 = samples[3 * samples.length / 4 - 1];
        double p100 = samples[samples.length - 1];


        int[] bins = new int[10];
        for (int sample : samples) {
            bins[sample]++;
        }

        Histogram histogram = new Histogram(new int[]{10}, new double[]{0.0}, new double[]{10.0});
        System.arraycopy(bins, 0, histogram.getBins(0), 0, 10);
        assertEquals(p0, histogram.getPTileThreshold(0.00001)[0], 1E-10);
        assertEquals(p50, histogram.getPTileThreshold(0.50)[0], 1E-10);
        assertEquals(p25, histogram.getPTileThreshold(0.25)[0], 1E-10);
        assertEquals(p75, histogram.getPTileThreshold(0.75)[0], 1E-10);
        assertEquals(p100, histogram.getPTileThreshold(0.99999)[0], 1E-10);
    }

}
