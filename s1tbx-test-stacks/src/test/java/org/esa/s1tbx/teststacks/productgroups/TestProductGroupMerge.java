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
package org.esa.s1tbx.teststacks.productgroups;

import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.ProductGroupMergeOp;
import org.esa.s1tbx.teststacks.StackTest;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assume.assumeTrue;


public class TestProductGroupMerge extends StackTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
    }

    @Override
    protected File createTmpFolder(final String folderName) {
        File folder = new File("c:\\tmp\\" + folderName);
        folder.mkdirs();
        return folder;
    }

    @Test
    public void testProductGroupMerge_Stack() throws Exception {
        final File tmpFolder = createTmpFolder("ProductGroupMerge_Stack");
        final List<Product> products = readProducts(asarSantoriniFolder);
        final List<Product> firstPair = products.subList(0, 2);

        File trgFolder = new File(tmpFolder,"stack1");
        Product stack1 = coregister(firstPair, trgFolder, "BEAM-DIMAP");

        final List<Product> firstThree = products.subList(0, 1);
        firstThree.add(products.get(2));
        trgFolder = new File(tmpFolder,"stack2");
        Product stack2 = coregister(firstThree, trgFolder, "BEAM-DIMAP");

        ProductGroupMergeOp merge = new ProductGroupMergeOp();
        merge.setSourceProducts(stack1, stack2);

        Product trgProduct = merge.getTargetProduct();

        trgFolder = new File(tmpFolder,"merged");
        ProductIO.writeProduct(trgProduct, trgFolder, "ProductGroup", true);

        stack1.dispose();
        stack2.dispose();
        //todo delete(tmpFolder);
    }

    @Test
    public void testProductGroupMerge_IfgStack() throws Exception {
        final File tmpFolder = createTmpFolder("ProductGroupMerge_IfgStack");
        final List<Product> products = readProducts(asarSantoriniFolder);
        final List<Product> firstPair = products.subList(0, 2);

        File trgFolder = new File(tmpFolder,"ifg_stack1");
        Product stack1 = coregisterInterferogram(firstPair, trgFolder, "BEAM-DIMAP");

        final List<Product> firstThree = products.subList(0, 1);
        firstThree.add(products.get(2));
        trgFolder = new File(tmpFolder,"ifg_stack2");
        Product stack2 = coregisterInterferogram(firstThree, trgFolder, "BEAM-DIMAP");

        ProductGroupMergeOp merge = new ProductGroupMergeOp();
        merge.setSourceProducts(stack1, stack2);

        Product trgProduct = merge.getTargetProduct();

        trgFolder = new File(tmpFolder,"ifg_merged");
        ProductIO.writeProduct(trgProduct, trgFolder, "ProductGroup", true);

        stack1.dispose();
        stack2.dispose();
        //todo delete(tmpFolder);
    }
}
