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
package org.esa.snap;

import junit.framework.TestCase;

import java.io.IOException;

public class TiepointSubsetTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testFail() throws IOException {
//        final String path = "I:\\Produkte\\ov2\\";
//        final String name = "MER_FR__1PNUPA20041101_175626_000000982031_00399_13980_0857.N1";
//        final String productPath = path + name;
//        final String tpName = "dem_alt";
//        final int startX = 23;
//        final int startY = 41;
//        final int width = 45;
//        final int height = 27;
//
//        final Product product = ProductIO.readProduct(productPath, null);
//        final RasterDataNode tpDemAlt = product.getTiePointGrid(tpName);
//        final float[] tpExpected = tpDemAlt.getPixels(startX, startY, width, height, new float[width * height]);
//
//        final ProductSubsetDef subsetDef = new ProductSubsetDef();
//        subsetDef.setRegion(startX, startY, width, height);
//
//        final Product subset = product.createSubset(subsetDef, "subset", "subs desc");
//        final RasterDataNode tpSubDemAlt = subset.getTiePointGrid(tpName);
//        final float[] tpActual = tpSubDemAlt.getPixels(0, 0, width, height, new float[width * height]);
//
//        for (int i = 0; i < tpExpected.length; i++) {
//            final float expected = tpExpected[i];
//            final float actual = tpActual[i];
//            System.out.println("i = " + i);
//            System.out.println((expected == actual) + " = " + expected + " == " + actual);
//            assertEquals(expected, actual, 1.0e-5f);
//        }
    }
}
