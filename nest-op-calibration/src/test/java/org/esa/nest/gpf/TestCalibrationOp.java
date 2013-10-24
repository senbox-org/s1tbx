/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.util.TestUtils;

/**
 * Unit test for Calibration Operator.
 */
public class TestCalibrationOp extends TestCase {

    private OperatorSpi spi;

    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_Calib.dim";

    private final static String inputPathIMP =     TestUtils.rootPathExpectedProducts+"\\input\\subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43.dim";
    private final static String expectedPathIMP =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43_Calib.dim";
    private final static String inputPathIMS =     TestUtils.rootPathExpectedProducts+"\\input\\subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34.dim";
    private final static String expectedPathIMS =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34_Calib.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U" };
    private String[] exceptionExemptions = { "not supported",
                                             "calibration has already been applied",
                                             "Cannot apply calibration to coregistered product"};

    @Override
    protected void setUp() throws Exception {
        spi = new CalibrationOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    public void testProcessingWSM() throws Exception {
        processFile(inputPathWSM, expectedPathWSM);
    }

    public void testProcessingIMP() throws Exception {
        processFile(inputPathIMP, expectedPathIMP);
    }

    public void testProcessingIMS() throws Exception {
        processFile(inputPathIMS, expectedPathIMS);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     * @param inputPath the path to the input product
     * @param expectedPath the path to the expected product
     * @throws Exception general exception
     */
    public void processFile(String inputPath, String expectedPath) throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPath);

        final CalibrationOp op = (CalibrationOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    //    TestUtils.compareProducts(op, targetProduct, expectedPath, null);
    // todo fix expected output files
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, null);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, null);
    }

    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, productTypeExemptions, null);
    }

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, productTypeExemptions, null);
    }

    public void testProcessAllCosmo() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllNestBox() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, exceptionExemptions);
    }
}