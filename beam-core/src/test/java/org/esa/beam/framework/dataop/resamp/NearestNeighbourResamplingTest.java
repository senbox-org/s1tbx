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

public class NearestNeighbourResamplingTest extends TestCase {

    final Resampling resampling = Resampling.NEAREST_NEIGHBOUR;
    final TestRaster raster = new TestRaster();

    public void testCreateIndex() {
        final Resampling.Index index = resampling.createIndex();
        assertNotNull(index);
        assertNotNull(index.i);
        assertNotNull(index.j);
        assertNotNull(index.ki);
        assertNotNull(index.kj);
        assertEquals(0, index.i.length);
        assertEquals(0, index.j.length);
        assertEquals(0, index.ki.length);
        assertEquals(0, index.kj.length);
    }

    public void testComputeIndexAndGetSample() throws Exception {
        final Resampling.Index index = resampling.createIndex();
        test(index, 0.5f, 0.0f, 0.0, 0.0, 10f);
        test(index, 0.5f, 2.0f, 0.0, 2.0, 10f);
        test(index, 4.5f, 0.0f, 4.0, 0.0, 50f);
        test(index, 0.5f, 3.9f, 0.0, 3.0, 20f);
        test(index, 2.5f, 1.0f, 2.0, 1.0, 30f);
        test(index, 4.5f, 4.0f, 4.0, 4.0, 70f);
        test(index, 2.9f, 2.9f, 2.0, 2.0, 20f);
    }

    private void test(final Resampling.Index index, float x, float y, double iExp, double jExp, float sampleExp) throws Exception {
        resampling.computeIndex(x, y, raster.getWidth(), raster.getHeight(), index);
        assertEquals(iExp, index.i0);
        assertEquals(jExp, index.j0);
        double sample = resampling.resample(raster, index);
        assertEquals(sampleExp, sample, 1e-5f);
    }

}
