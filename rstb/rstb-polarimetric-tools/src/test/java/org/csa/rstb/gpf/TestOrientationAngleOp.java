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
package org.csa.rstb.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for OrientationAngleOp.
 */
public class TestOrientationAngleOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new OrientationAngleCorrectionOp.Spi();

    private final static String inputPathQuad = TestUtils.rootPathExpectedProducts + "\\input\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim";
    private final static String inputQuadFullStack = TestUtils.rootPathExpectedProducts + "\\input\\QuadPolStack\\RS2-Quad_Pol_Stack.dim";
    private final static String inputC3Stack = TestUtils.rootPathExpectedProducts + "\\input\\QuadPolStack\\RS2-C3-Stack.dim";
    private final static String inputT3Stack = TestUtils.rootPathExpectedProducts + "\\input\\QuadPolStack\\RS2-T3-Stack.dim";

    private Product runOrientation(final OrientationAngleCorrectionOp op, final String path) throws Exception {
        final File inputFile = new File(path);
        if (!inputFile.exists()) {
            TestUtils.skipTest(this);
            return null;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        return targetProduct;
    }

    @Test
    public void testOrientationAngle() throws Exception {

        runOrientation((OrientationAngleCorrectionOp) spi.createOperator(), inputPathQuad);
        runOrientation((OrientationAngleCorrectionOp) spi.createOperator(), inputQuadFullStack);
        runOrientation((OrientationAngleCorrectionOp) spi.createOperator(), inputC3Stack);
        runOrientation((OrientationAngleCorrectionOp) spi.createOperator(), inputT3Stack);
    }
}