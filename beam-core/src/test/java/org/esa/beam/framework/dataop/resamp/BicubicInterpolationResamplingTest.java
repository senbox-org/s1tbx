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

package org.esa.beam.framework.dataop.resamp;

import junit.framework.TestCase;

public class BicubicInterpolationResamplingTest extends TestCase {

    final Resampling resampling = Resampling.CUBIC_CONVOLUTION;
    final TestRaster raster = new TestRaster();

    public void testCreateIndex() {
        final Resampling.Index index = resampling.createIndex();
        assertNotNull(index);
        assertNotNull(index.i);
        assertNotNull(index);
        assertNotNull(index.i);
        assertNotNull(index.j);
        assertNotNull(index.ki);
        assertNotNull(index.kj);
        assertEquals(4, index.i.length);
        assertEquals(4, index.j.length);
        assertEquals(4, index.ki.length);
        assertEquals(4, index.kj.length);
    }

    public void testIndexAndSample() throws Exception {
        final Resampling.Index index = resampling.createIndex();

        testIndexAndSample(
                index,
                2.2f, 2.3f,
                0, 1, 2, 3,
                0, 1, 2, 3,
                -0.063f, 0.363f, 0.847f, -0.147f,
                -0.032f, 0.232f, 0.928f, -0.128f,
                25.69918f);
    }

    private void testIndexAndSample(
            final Resampling.Index index,
            float x, float y,
            int i1Exp, int i2Exp, int i3Exp, int i4Exp,
            int j1Exp, int j2Exp, int j3Exp, int j4Exp,
            float ki1Exp, float ki2Exp, float ki3Exp, float ki4Exp,
            float kj1Exp, float kj2Exp, float kj3Exp, float kj4Exp,
            float sampleExp) throws Exception {

        resampling.computeIndex(x, y, raster.getWidth(), raster.getHeight(), index);

        assertEquals(i1Exp, index.i[0]);
        assertEquals(i2Exp, index.i[1]);
        assertEquals(i3Exp, index.i[2]);
        assertEquals(i4Exp, index.i[3]);

        assertEquals(j1Exp, index.j[0]);
        assertEquals(j2Exp, index.j[1]);
        assertEquals(j3Exp, index.j[2]);
        assertEquals(j4Exp, index.j[3]);

        assertEquals(ki1Exp, index.ki[0], 1e-5f);
        assertEquals(ki2Exp, index.ki[1], 1e-5f);
        assertEquals(ki3Exp, index.ki[2], 1e-5f);
        assertEquals(ki4Exp, index.ki[3], 1e-5f);

        assertEquals(kj1Exp, index.kj[0], 1e-5f);
        assertEquals(kj2Exp, index.kj[1], 1e-5f);
        assertEquals(kj3Exp, index.kj[2], 1e-5f);
        assertEquals(kj4Exp, index.kj[3], 1e-5f);

        float sample = resampling.resample(raster, index);
        assertEquals(sampleExp, sample, 1e-5f);
    }
}
