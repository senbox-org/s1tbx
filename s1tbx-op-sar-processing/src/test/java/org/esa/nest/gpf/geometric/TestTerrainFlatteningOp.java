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
package org.esa.nest.gpf.geometric;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Range Doppler.
 */
public class TestTerrainFlatteningOp {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new TerrainFlatteningOp.Spi();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"not supported", "not be map projected", "outside of SRTM valid area"};

    /**
     * Processes a WSM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessWSM() throws Exception {
        final File inputFile = TestData.inputASAR_WSM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 4351.9638671875f,2904.682861328125f,2189.5703125f,2127.906494140625f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 200, 200, expected);
    }

    /**
     * Processes a IMS product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessIMS() throws Exception {
        final File inputFile = TestData.inputASAR_IMS;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    }

    /**
     * Processes a APM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessAPM() throws Exception {
        final File inputFile = TestData.inputASAR_APM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    }

    /**
     * Processes a APM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessCalibratedAPM() throws Exception {
        final File inputFile = TestData.inputASAR_APM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    }
}
