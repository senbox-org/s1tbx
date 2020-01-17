/*
 * Copyright (C) 2019 by SkyWatch Space Applications http://www.skywatch.com
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
package org.csa.rstb.calibration;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test for Calibration Operator.
 */
public class TestCalibrationOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new CalibrationOp.Spi();
    private TestProcessor testProcessor;

    private String[] productTypeExemptions = {"GeoTIFF"};

    @Before
    public void setUp() {
        testProcessor = S1TBXTests.createS1TBXTestProcessor();

        // If any of the file does not exist: the test will be ignored
        assumeTrue(TestData.inputRS2_SQuad + "not found", TestData.inputRS2_SQuad.exists());
    }

    @Test
    public void testProcessingRS2_Quad() throws Exception {

        final float[] expected = new float[] {0.030368317f, 0.004051784f, 0.04810727f};
        processFile(TestData.inputRS2_SQuad, "sigma0_VV", expected);
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
    public void testProcessAllRadarsat2() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsRadarsat2, "RADARSAT-2", productTypeExemptions, null);
    }
}
