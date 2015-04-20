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
package org.esa.s1tbx.gpf;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit test for Calibration Operator.
 */
public class TestRemoveAntennaPatternOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new RemoveAntennaPatternOp.Spi();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported",
            "ASA_IMS_1P is not a valid ASAR product type for the operator",
            "calibration has already been applied",
            "Cannot apply calibration to coregistered product"};

    @Test
    public void testProcessingASAR_WSM() throws Exception {
        processFile(TestData.inputASAR_WSM);
    }

    @Test
    public void testProcessingERS_IMP() throws Exception {
        processFile(TestData.inputERS_IMP);
    }

    @Test
    public void testProcessingERS_IMS() throws Exception {
        processFile(TestData.inputERS_IMS);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputFile    the path to the input product
     * @throws Exception general exception
     */
    private void processFile(final File inputFile) throws Exception {
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final RemoveAntennaPatternOp op = (RemoveAntennaPatternOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsASAR, "ENVISAT", productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsERS, "ERS CEOS", productTypeExemptions, exceptionExemptions);
    }
}
