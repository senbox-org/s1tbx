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
package org.csa.rstb.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.util.TestUtils;

/**
 * Unit test for PolarimetricMatricesOp
 */
public class TestPolarimetricMatricesOp extends TestCase {

    private OperatorSpi spi;

    private final static String quadInputPath =        TestUtils.rootPathExpectedProducts+"\\input\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim";
    private final static String inputQuadFullStack =   TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-Quad_Pol_Stack.dim";
    
    private final static String expectedPathC3 =  TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_C3.dim";
    private final static String expectedPathT3 =  TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_T3.dim";

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new PolarimetricMatricesOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    private static Product runMatrix(final PolarimetricMatricesOp op,
                                            final String decompositionName, final String path) throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(path);
        
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetMatrixType(decompositionName);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        return targetProduct;
    }

    /**
     * Compute covariance matrix C3 from a Radarsat-2 product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testComputeC3() throws Exception {

        final PolarimetricMatricesOp op = (PolarimetricMatricesOp)spi.createOperator();
        final Product targetProduct = runMatrix(op, PolarimetricMatricesOp.C3, quadInputPath);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedPathC3, null);
    }

    /**
     * Compute coherency matrix T3 from a Radarsat-2 product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testComputeT3() throws Exception {

        final PolarimetricMatricesOp op = (PolarimetricMatricesOp)spi.createOperator();
        final Product targetProduct = runMatrix(op, PolarimetricMatricesOp.T3, quadInputPath);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedPathT3, null);
    }

    /**
     * Compute covariance matrix C4 from a Radarsat-2 product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testComputeC4() throws Exception {

        final PolarimetricMatricesOp op = (PolarimetricMatricesOp)spi.createOperator();
        final Product targetProduct = runMatrix(op, PolarimetricMatricesOp.C4, quadInputPath);
    }

    /**
     * Compute coherency matrix T4 from a Radarsat-2 product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testComputeT4() throws Exception {

        final PolarimetricMatricesOp op = (PolarimetricMatricesOp)spi.createOperator();
        final Product targetProduct = runMatrix(op, PolarimetricMatricesOp.T4, quadInputPath);
    }

    /**
     * Compute coherency matrix T3 from a stack of Quad Pol Radarsat-2 products
     * @throws Exception general exception
     */
    public void testQuadPolStack() throws Exception {

        runMatrix((PolarimetricMatricesOp)spi.createOperator(), PolarimetricMatricesOp.T3, inputQuadFullStack);
        runMatrix((PolarimetricMatricesOp)spi.createOperator(), PolarimetricMatricesOp.C3, inputQuadFullStack);
        runMatrix((PolarimetricMatricesOp)spi.createOperator(), PolarimetricMatricesOp.T4, inputQuadFullStack);
        runMatrix((PolarimetricMatricesOp)spi.createOperator(), PolarimetricMatricesOp.C4, inputQuadFullStack);
    }
}
