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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Calibration Operator.
 */
public class TestCalibrationOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new CalibrationOp.Spi();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported",
            "calibration has already been applied",
            "Cannot apply calibration to coregistered product"};

    @Test
    public void testProcessingASAR_WSM() throws Exception {
        final File inputFile = TestData.inputASAR_WSM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(0.027908697724342346, floatValues[0], 0.0001);
        assertEquals(0.019894488155841827, floatValues[1], 0.0001);
        assertEquals(0.020605698227882385, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingASAR_IMS() throws Exception {
        final File inputFile = TestData.inputASAR_IMS;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(0.043132662773132324, floatValues[0], 0.0001);
        assertEquals(3.3039296977221966E-4, floatValues[1], 0.0001);
        assertEquals(0.06897620856761932, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingERS_IMP() throws Exception {
        final File inputFile = TestData.inputERS_IMP;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(0.19550558924674988, floatValues[0], 0.0001);
        assertEquals(0.19353462755680084, floatValues[1], 0.0001);
        assertEquals(0.1338571310043335, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingERS_IMS() throws Exception {
        final File inputFile = TestData.inputERS_IMS;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(0.11003237217664719, floatValues[0], 0.0001);
        assertEquals(0.09509188681840897, floatValues[1], 0.0001);
        assertEquals(0.01090210024267435, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingS1_GRD() throws Exception {
        final File inputFile = TestData.inputS1_GRD;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(0.015372134745121002, floatValues[0], 0.0001);
        assertEquals(0.01537325419485569, floatValues[1], 0.0001);
        assertEquals(0.02168026752769947, floatValues[2], 0.0001);
    }

    @Test
    public void testProcessingS1_StripmapSLC() throws Exception {
        final File inputFile = TestData.inputS1_StripmapSLC;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product targetProduct = processFile(inputFile);

        final Band band = targetProduct.getBand("sigma0_VV");
        assertNotNull(band);

        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        assertEquals(4.557766437530518, floatValues[0], 0.0001);
        assertEquals(17.115325927734375, floatValues[1], 0.0001);
        assertEquals(43.94808578491211, floatValues[2], 0.0001);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputFile the path to the input product
     * @throws Exception general exception
     */
    private static Product processFile(final File inputFile) throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final CalibrationOp op = (CalibrationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        return targetProduct;
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, null);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, null);
    }

    @Test
    public void testProcessAllALOS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, productTypeExemptions, null);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, productTypeExemptions, null);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathSentinel1, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllNestBox() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, exceptionExemptions);
    }
}