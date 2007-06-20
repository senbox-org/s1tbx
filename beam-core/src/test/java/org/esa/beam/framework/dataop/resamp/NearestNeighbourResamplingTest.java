/*
 * $Id: NearestNeighbourResamplingTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
        test(index, 0.5f, 0.0f, 0, 0, 10f);
        test(index, 0.5f, 2.0f, 0, 2, 10f);
        test(index, 4.5f, 0.0f, 4, 0, 50f);
        test(index, 0.5f, 3.9f, 0, 3, 20f);
        test(index, 2.5f, 1.0f, 2, 1, 30f);
        test(index, 4.5f, 4.0f, 4, 4, 70f);
    }

    private void test(final Resampling.Index index, float x, float y, int iExp, int jExp, float sampleExp) throws Exception {
        resampling.computeIndex(x, y, raster.getWidth(), raster.getHeight(), index);
        assertEquals(iExp, index.i0);
        assertEquals(jExp, index.j0);
        float sample = resampling.resample(raster, index);
        assertEquals(sampleExp, sample, 1e-5f);
    }

}
