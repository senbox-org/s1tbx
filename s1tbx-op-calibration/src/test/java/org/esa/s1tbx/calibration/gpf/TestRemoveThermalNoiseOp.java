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
package org.esa.s1tbx.calibration.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.S1TBXTests;
import org.esa.s1tbx.TestData;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.gpf.TestProcessor;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for RemoveThermalNoise Operator.
 */
public class TestRemoveThermalNoiseOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new Sentinel1RemoveThermalNoiseOp.Spi();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported",
            "not a valid mission for Sentinel1 product"};

    @Test
    public void testProcessingS1_GRD() throws Exception {
        final File inputFile = TestData.inputS1_GRD;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("Intensity_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(1024.0, floatValues[0], 0.0001);
        assertEquals(1024.0, floatValues[1], 0.0001);
        assertEquals(1444.0, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingS1_StripmapSLC() throws Exception {
        final File inputFile = TestData.inputS1_StripmapSLC;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("Intensity_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(629.0, floatValues[0], 0.0001);
        assertEquals(2362.0, floatValues[1], 0.0001);
        assertEquals(6065.0, floatValues[2], 0.0001);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputFile the path to the input product
     * @throws Exception general exception
     */
    private static Product processFile(final File inputFile) throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final Sentinel1RemoveThermalNoiseOp op = (Sentinel1RemoveThermalNoiseOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);
        return targetProduct;
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsSentinel1, "SENTINEL-1", productTypeExemptions, exceptionExemptions);
    }
}
