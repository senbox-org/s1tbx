/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.TiePointGrid;
import org.esa.s1tbx.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Test performance of tie point grid geocoding
 */
public class TestTiePoints {

    static {
        TestUtils.initTestEnvironment();
    }

    private final File inputFile = TestData.inputASAR_WSM;

    private Product product1 = null;
    private Product product2 = null;

    @Before
    public void setUp() throws Exception {
        if (inputFile.exists()) {
            product1 = ProductIO.readProduct(inputFile);
            product2 = ProductIO.readProduct(inputFile);
        }
    }

    @Test
    public void testGetPixelDouble() throws Exception {
        if (product1 == null) {
            TestUtils.skipTest(this, product1 +" not found");
            return;
        }
        TiePointGrid tpg = product1.getTiePointGridAt(0);
        int w = product1.getSceneRasterWidth();
        int h = product1.getSceneRasterHeight();

        double[] floats1 = new double[w * h];
        int i = 0;
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                floats1[i++] = tpg.getPixelDouble(x, y);
            }
        }
    }

    @Test
    public void testGetPixels() throws Exception {
        if (product2 == null) {
            TestUtils.skipTest(this, product2 +" not found");
            return;
        }
        TiePointGrid tpg = product2.getTiePointGridAt(0);
        int w = product2.getSceneRasterWidth();
        int h = product2.getSceneRasterHeight();

        double[] floats = new double[w * h];
        tpg.getPixels(0, 0, w, h, floats, ProgressMonitor.NULL);
    }

    @Test
    public void testCompareFloats() throws Exception {

        if (product1 == null) {
            TestUtils.skipTest(this, product1 +" not found");
            return;
        }
        final TiePointGrid tpg = product1.getTiePointGridAt(0);
        int w = product1.getSceneRasterWidth();
        int h = product1.getSceneRasterHeight();

        final double[] floats = new double[w * h];
        tpg.getPixels(0, 0, w, h, floats, ProgressMonitor.NULL);

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                final double f = tpg.getPixelDouble(x, y);
                assertTrue(f == floats[y * w + x]);
            }
        }
    }
}
