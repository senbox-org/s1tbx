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
import org.esa.s1tbx.commons.test.ProductValidator;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test for CreateStackOp.
 */
@RunWith(LongTestRunner.class)
public class TestCreateStackOp extends ProcessorTest {

    private final static File asarBamFile1 = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Bam/ASA_IMS_1PNUPA20031203_061259_000000162022_00120_09192_0099.N1");
    private final static File asarBamFile2 = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Bam/ASA_IMS_1PXPDE20040211_061300_000000142024_00120_10194_0013.N1");
    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarBamFile1 + " not found", asarBamFile1.exists());
        assumeTrue(asarBamFile2 + " not found", asarBamFile2.exists());
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
    }

    @Test
    public void testForumIssue() throws IOException {

        Product[] products = new Product[2];
        products[0] = ProductIO.readProduct(asarBamFile1);
        products[1] = ProductIO.readProduct(asarBamFile2);
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

        outProduct.dispose();
        delete(tmpFolder);
    }

    @Test
    public void testStack1() throws Exception {
        final List<Product> products = readProducts(asarSantoriniFolder);

        CreateStackOp createStack = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStack.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        Product prod = createStack.getTargetProduct();

        final ProductValidator validator = new ProductValidator(prod);
        validator.validateProduct();
        validator.validateBands(new String[] {
                "i_VV_mst_25Feb2004", "q_VV_mst_25Feb2004",
                "i_VV_slv1_31Mar2004", "q_VV_slv1_31Mar2004",
                "i_VV_slv2_14Jul2004", "q_VV_slv2_14Jul2004",
                "i_VV_slv3_27Oct2004", "q_VV_slv3_27Oct2004"
        });

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(prod, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        prod.dispose();
        delete(tmpFolder);
    }
}
