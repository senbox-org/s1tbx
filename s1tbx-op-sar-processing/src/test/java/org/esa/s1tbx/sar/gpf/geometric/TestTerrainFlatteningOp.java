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
package org.esa.s1tbx.sar.gpf.geometric;


import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test for Range Doppler.
 */
public class TestTerrainFlatteningOp {

    private final static File inputFile1 = TestData.inputASAR_WSM;
    private final static File inputFile2 = TestData.inputASAR_IMS;
    private final static File inputFile3 = TestData.inputASAR_APM;
    private final static File inputFile4 = TestData.inputASAR_APM;

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputFile1 + " not found", inputFile1.exists());
        assumeTrue(inputFile2 + " not found", inputFile2.exists());
        assumeTrue(inputFile3 + " not found", inputFile3.exists());
        assumeTrue(inputFile4 + " not found", inputFile4.exists());
    }

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new TerrainFlatteningOp.Spi();

    /**
     * Processes a WSM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessWSM() throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile1);

        final CalibrationOp calOp = new CalibrationOp();
        calOp.setSourceProduct(sourceProduct);
        calOp.setParameter("outputBetaBand", true);
        calOp.setParameter("createBetaBand", true);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(calOp.getTargetProduct());

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 0.63482225f, 0.79944354f, 0.16941425f, 0.069053054f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 200, 200, expected);
    }

    /**
     * Processes a IMS product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessIMS() throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile2);

        final CalibrationOp calOp = new CalibrationOp();
        calOp.setSourceProduct(sourceProduct);
        calOp.setParameter("outputBetaBand", true);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(calOp.getTargetProduct());

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
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile3);

        final CalibrationOp calOp = new CalibrationOp();
        calOp.setSourceProduct(sourceProduct);
        calOp.setParameter("outputBetaBand", true);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(calOp.getTargetProduct());

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
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile4);

        final CalibrationOp calOp = new CalibrationOp();
        calOp.setSourceProduct(sourceProduct);
        calOp.setParameter("outputBetaBand", true);

        final TerrainFlatteningOp op = (TerrainFlatteningOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(calOp.getTargetProduct());

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
    }
}
