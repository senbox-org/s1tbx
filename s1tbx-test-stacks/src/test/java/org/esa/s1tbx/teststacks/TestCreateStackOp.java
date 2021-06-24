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
package org.esa.s1tbx.teststacks;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for CreateStackOp.
 */
@RunWith(LongTestRunner.class)
public class TestCreateStackOp extends ProcessorTest {

    private final static File inFile1 = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Bam/ASA_IMS_1PNUPA20031203_061259_000000162022_00120_09192_0099.N1");
    private final static File inFile2 = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Bam/ASA_IMS_1PXPDE20040211_061300_000000142024_00120_10194_0013.N1");

    @Test
    public void testForumIssue() throws IOException {
        if(!inFile1.exists() || !inFile2.exists()){
            return;
        }

        Product[] products = new Product[2];
        products[0] = ProductIO.readProduct(inFile1);
        products[1] = ProductIO.readProduct(inFile2);
        assertNotNull(products[0]);
        assertNotNull(products[1]);

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("extent","Master");
        parameters.put("initialOffsetMethod","Product Geolocation");
        parameters.put("masterBands",products[0].getBandAt(0).getName()+","+products[0].getBandAt(1).getName());
        parameters.put("resamplingType","NEAREST_NEIGHBOUR");
        parameters.put("sourceBands",products[1].getBandAt(0).getName()+","+products[1].getBandAt(1).getName());

        System.out.println("Creating stack...");
        Product outProduct = GPF.createProduct("CreateStack", parameters, products);

        File tmpFolder = createTmpFolder("stacks");
        ProductIO.writeProduct(outProduct, new File(tmpFolder,"target.dim"), "BEAM-DIMAP", true);

        tmpFolder.delete();
    }

    private Product[] readProducts(final File folder) throws IOException {
        assertTrue(folder.isDirectory());
        File[] files = folder.listFiles();
        final List<Product> productList = new ArrayList<>();
        if(files != null) {
            for(File file : files) {
                Product product = ProductIO.readProduct(file);
                if(product != null) {
                    productList.add(product);
                }
            }
        }
        return productList.toArray(new Product[0]);
    }

    @Test
    public void testStack1() throws IOException {
        final Product[] products = readProducts(new File("E:\\EO\\RS2\\ASMERS\\ManitobaFrame"));

        CreateStackOp createStackOp = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStackOp.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        Product outProduct = createStackOp.getTargetProduct();

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(outProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        //tmpFolder.delete();
    }
}
