/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.teststacks.coregistration;

import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.teststacks.StackTest;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(LongTestRunner.class)
public class TestCrossCorrelationCoregistrationStack extends StackTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");
    private final static File rs2ManitobaFolder = new File(S1TBXTests.inputPathProperty + "/SAR/RS2/Manitoba");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
        assumeTrue(rs2ManitobaFolder + " not found", rs2ManitobaFolder.exists());
    }

    @Test
    public void testStackSantorini() throws Exception {
        final List<Product> products = readProducts(asarSantoriniFolder);

        final Product trgProduct = coregister(products);

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        MetadataElement warpData = getWarpData(trgProduct, "Band_i_VV_slv3_27Oct2004");
        assertEquals("rmsMean", 1.1229252748775072E-13, warpData.getAttributeDouble("rmsMean"), 0.0001);
        assertEquals("rmsStd", 1.3035429283962273E-13, warpData.getAttributeDouble("rmsStd"), 0.0001);

        trgProduct.dispose();
        delete(tmpFolder);
    }

    @Test
    @Ignore
    public void testStackQPManitoba() throws Exception {
        final List<Product> products = readProducts(rs2ManitobaFolder);
        final List<Product> firstPair = products.subList(0, 2);

        final Product trgProduct = coregister(firstPair);

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        MetadataElement warpData = getWarpData(trgProduct, "Band_i_VV_slv3_27Oct2004");
        assertEquals("rmsMean", 1, warpData.getAttributeDouble("rmsMean"), 0.0001);
        assertEquals("rmsStd", 1, warpData.getAttributeDouble("rmsStd"), 0.0001);

        trgProduct.dispose();
        delete(tmpFolder);
    }

    private static MetadataElement getWarpData(final Product product, final String bandName) {
        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        assertNotNull(absRoot);
        MetadataElement bandMeta = absRoot.getElement(bandName);
        assertNotNull(bandMeta);
        MetadataElement warpData = bandMeta.getElement("WarpData");
        assertNotNull(bandMeta);
        return warpData;
    }
}
