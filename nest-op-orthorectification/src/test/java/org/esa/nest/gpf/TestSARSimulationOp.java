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
 * Unit test for SAR Simulation Operator.
 */
public class TestSARSimulationOp extends TestCase {

    private OperatorSpi spi;
    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_SIM.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX" };
    private String[] exceptionExemptions = { "not supported", "already map projected" };

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new SARSimulationOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessing() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathWSM);

        final SARSimulationOp op = (SARSimulationOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathWSM, null);
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, null, exceptionExemptions);
    }

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, null, exceptionExemptions);
    }

    public void testProcessAllTerraSARX() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, null, exceptionExemptions);
    }

    public void testProcessAllCosmo() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, null, exceptionExemptions);
    }

    public void testProcessAllNestBox() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, exceptionExemptions);
    }
}
