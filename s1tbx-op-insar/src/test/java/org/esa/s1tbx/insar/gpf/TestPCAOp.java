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
package org.esa.s1tbx.insar.gpf;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.s1tbx.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit test for Calibration Operator.
 */
public class TestPCAOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new PCAOp.Spi();

    @Test
    public void testProcessingIMS() throws Exception {
        processFile(TestData.inputStackIMS);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputFile    the path to the input product
     * @throws Exception general exception
     */
    public void processFile(File inputFile) throws Exception {
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final PCAOp op = (PCAOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 1.1567158E7f,1.1564227E7f,1.1563969E7f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), expected);
    }

}
