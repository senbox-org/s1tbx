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

        final float[] expected = new float[] {0.027908697724342346f, 0.019894488155841827f, 0.020605698227882385f};
        processFile(TestData.inputASAR_WSM, "sigma0_VV", expected);
    }

    @Test
    public void testProcessingASAR_IMS() throws Exception {

        final float[] expected = new float[] {0.043132662773132324f, 3.3039296977221966E-4f, 0.06897620856761932f};
        processFile(TestData.inputASAR_IMS, "sigma0_VV", expected);
    }

    @Test
    public void testProcessingERS_IMP() throws Exception {

        final float[] expected = new float[] {0.19550558924674988f, 0.19353462755680084f, 0.1338571310043335f};
        processFile(TestData.inputERS_IMP, "sigma0_VV", expected);
    }

    @Test
    public void testProcessingERS_IMS() throws Exception {

        final float[] expected = new float[] {0.11003237217664719f, 0.09509188681840897f, 0.01090210024267435f};
        processFile(TestData.inputERS_IMS, "sigma0_VV", expected);
    }

    @Test
    public void testProcessingS1_GRD() throws Exception {

        final float[] expected = new float[] {0.015372134745121002f, 0.01537325419485569f, 0.02168026752769947f};
        processFile(TestData.inputS1_GRD, "sigma0_VV", expected);
    }

    @Test
    public void testProcessingS1_StripmapSLC() throws Exception {

        final float[] expected = new float[] {4.557766437530518f, 17.115325927734375f, 43.94808578491211f};
        processFile(TestData.inputS1_StripmapSLC, "sigma0_VV", expected);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputFile the path to the input product
     * @param bandName the target band name to verify
     * @param expected expected values
     * @throws Exception general exception
     */
    private void processFile(final File inputFile, final String bandName, final float[] expected) throws Exception {
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final CalibrationOp op = (CalibrationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        TestUtils.comparePixels(targetProduct, bandName, expected);
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