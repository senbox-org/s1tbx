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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for RemoveThermalNoise Operator.
 */
public class TestRemoveThermalNoiseOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new Sentinel1RemoveThermalNoiseOp.Spi();

    final String s1ZipFilePath = TestUtils.rootPathTestProducts+"input\\S1\\S1A_S1_GRDM_1SDV_20140607T172812_20140607T172836_000947_000EBD_7543.zip";

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported",
            "calibration has already been applied",
            "Cannot apply calibration to coregistered product"};

    @Test
    public void testProcessingGRD() throws Exception {
        processFile(s1ZipFilePath, null);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputPath    the path to the input product
     * @param expectedPath the path to the expected product
     * @throws Exception general exception
     */
    public void processFile(String inputPath, String expectedPath) throws Exception {
        final File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputPath + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final Sentinel1RemoveThermalNoiseOp op = (Sentinel1RemoveThermalNoiseOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathSentinel1, productTypeExemptions, exceptionExemptions);
    }
}