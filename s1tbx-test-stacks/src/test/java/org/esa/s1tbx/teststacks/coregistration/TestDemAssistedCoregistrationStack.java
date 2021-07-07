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

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.DEMAssistedCoregistrationOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assume.assumeTrue;

public class TestDemAssistedCoregistrationStack extends ProcessorTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
    }

    @Test
    public void testStack1() throws Exception {
        final List<Product> products = readProducts(asarSantoriniFolder);

        DEMAssistedCoregistrationOp demAssistedCoregistration = new DEMAssistedCoregistrationOp();
        int cnt = 0;
        for(Product product : products) {
            demAssistedCoregistration.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        Product trgProduct = demAssistedCoregistration.getTargetProduct();

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        trgProduct.dispose();
        delete(tmpFolder);
    }
}
