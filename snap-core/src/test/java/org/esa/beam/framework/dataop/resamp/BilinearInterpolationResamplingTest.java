/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

public class BilinearInterpolationResamplingTest extends TestCase {

    final Resampling resampling = Resampling.BILINEAR_INTERPOLATION;
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
        assertEquals(2, index.i.length);
        assertEquals(2, index.j.length);
        assertEquals(1, index.ki.length);
        assertEquals(1, index.kj.length);
    }

    public void testIndexAndSample() throws Exception {
        final Resampling.Index index = resampling.createIndex();

        testIndexAndSample(index, -.5f, 0.0f, 0.0, 0.0, 0.0, 0.0, 0.0f, 0.5f, 10.0f);

        testIndexAndSample(index, 0.0f, 0.0f, 0.0, 0.0, 0.0, 0.0, 0.5f, 0.5f, 10.0f);
        testIndexAndSample(index, 0.5f, 0.0f, 0.0, 1.0, 0.0, 0.0, 0.0f, 0.5f, 10.0f);
        testIndexAndSample(index, 1.0f, 0.0f, 0.0, 1.0, 0.0, 0.0, 0.5f, 0.5f, 15.0f);
        testIndexAndSample(index, 1.5f, 0.0f, 1.0, 2.0, 0.0, 0.0, 0.0f, 0.5f, 20.0f);
        testIndexAndSample(index, 2.0f, 0.0f, 1.0, 2.0, 0.0, 0.0, 0.5f, 0.5f, 25.0f);
        testIndexAndSample(index, 2.5f, 0.0f, 2.0, 3.0, 0.0, 0.0, 0.0f, 0.5f, 30.0f);
        testIndexAndSample(index, 3.0f, 0.0f, 2.0, 3.0, 0.0, 0.0, 0.5f, 0.5f, 35.0f);
        testIndexAndSample(index, 3.5f, 0.0f, 3.0, 4.0, 0.0, 0.0, 0.0f, 0.5f, 40.0f);
        testIndexAndSample(index, 4.0f, 0.0f, 3.0, 4.0, 0.0, 0.0, 0.5f, 0.5f, 45.0f);
        testIndexAndSample(index, 4.5f, 0.0f, 4.0, 4.0, 0.0, 0.0, 0.0f, 0.5f, 50.0f);

        testIndexAndSample(index, 5.0f, 0.0f, 4, 4, 0, 0, 0.5f, 0.5f, 50.0f);
        testIndexAndSample(index, 5.5f, 0.0f, 4, 4, 0, 0, 0.0f, 0.5f, 50.0f);

        testIndexAndSample(index, 2.2f, 2.3f, 1, 2, 1, 2, 0.7f, 0.8f, 26.2f);
    }

    private void testIndexAndSample(final Resampling.Index index,
                                    float x, float y,
                                    double i1Exp, double i2Exp,
                                    double j1Exp, double j2Exp,
                                    float kiExp,
                                    float kjExp,
                                    float sampleExp) throws Exception {
        resampling.computeIndex(x, y, raster.getWidth(), raster.getHeight(), index);
        assertEquals(i1Exp, index.i[0]);
        assertEquals(i2Exp, index.i[1]);
        assertEquals(j1Exp, index.j[0]);
        assertEquals(j2Exp, index.j[1]);
        assertEquals(kiExp, index.ki[0], 1e-5f);
        assertEquals(kjExp, index.kj[0], 1e-5f);
        double sample = resampling.resample(raster, index);
        assertEquals(sampleExp, sample, 1e-5f);
    }
}
