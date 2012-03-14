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

import org.junit.Test;

import javax.media.jai.Histogram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Norman
 */
public class StxTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMinIsNan() throws Exception {
        new Stx(Double.NaN, 1.0, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMaxIsNan() throws Exception {
        new Stx(0.0, Double.NaN, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorResolutionLevelIsInvalid() throws Exception {
        new Stx(0.0, 1.0, Double.NaN, Double.NaN, false, false, new Histogram(256, -1, 1, 1), -2);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorHistogramIsNull() throws Exception {
        new Stx(0.0, 1.0, Double.NaN, Double.NaN, false, false, (Histogram) null, 0);
    }

    @Test
    public void testConstructor() throws Exception {
        final Histogram histogram = new Histogram(256, -1, 1, 1);

        final Stx stx = new Stx(-1.0, 1.0, Double.NaN, Double.NaN, false, false, histogram, 0);

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
}
