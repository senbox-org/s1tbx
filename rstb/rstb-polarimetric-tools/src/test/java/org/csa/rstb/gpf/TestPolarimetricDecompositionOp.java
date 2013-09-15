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
 * Unit test for PolarimetricDecompositionOp.
 */
public class TestPolarimetricDecompositionOp extends TestCase {

    private OperatorSpi spi;

    private final static String inputPathQuad =         TestUtils.rootPathExpectedProducts+"\\input\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim";
    private final static String inputQuadFullStack =    TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-Quad_Pol_Stack.dim";
    private final static String inputC3Stack =          TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-C3-Stack.dim";
    private final static String inputT3Stack =          TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-T3-Stack.dim";

    private final static String expectedSinclair =  TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_Sinclair.dim";
    private final static String expectedPauli =     TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_Pauli.dim";
    private final static String expectedFreeman =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_FreemanDurden.dim";
    private final static String expectedYamaguchi =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_Yamaguchi.dim";
    private final static String expectedVanZyl =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_VanZyl.dim";
    private final static String expectedCloude =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_Cloude.dim";
    private final static String expectedHaAlpha =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_HaAlpha.dim";
    private final static String expectedTouzi =   TestUtils.rootPathExpectedProducts+"\\expected\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900_Touzi.dim";

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new PolarimetricDecompositionOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    private static Product runDecomposition(final PolarimetricDecompositionOp op,
                                            final String decompositionName, final String path) throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(path);

        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetDecomposition(decompositionName);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        return targetProduct;
    }

    /**
     * Perform Sinclair decomposition of a Radarsat-2 product and compares it with processed product known to be correct
     * @throws Exception general exception
     */
    public void testSinclairDecomposition() throws Exception {
        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedSinclair, null);
    }

    /**
     * Perform Pauli decomposition of a Radarsat-2 product and compares it with processed product known to be correct
     * @throws Exception general exception
     */
    public void testPauliDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.PAULI_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedPauli, null);
    }

    /**
     * Perform Freeman-Durden decomposition of a Radarsat-2 product and compares it with processed product known to be correct
     * @throws Exception general exception
     */
    public void testFreemanDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedFreeman, null);
    }

    public void testCloudeDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedCloude, null);
    }

    /**
     * Perform H-A-Alpha decomposition of a Radarsat-2 product and compares it with processed product known to be correct
     * @throws Exception general exception
     */
    public void testHAAlphaDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setHAAlphaParameters(true, true, true, true);
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedHaAlpha, null);
    }

    public void testTouziDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setTouziParameters(true, true, true, true);
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.TOUZI_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedTouzi, null);
    }

    public void testVanZylDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.VANZYL_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedVanZyl, null);
    }

    public void testYamaguchiDecomposition() throws Exception {

        final PolarimetricDecompositionOp op = (PolarimetricDecompositionOp)spi.createOperator();
        final Product targetProduct = runDecomposition(op,
                PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION, inputPathQuad);
        if(targetProduct != null)
            TestUtils.compareProducts(targetProduct, expectedYamaguchi, null);
    }

    // Quad Pol Stack

    public void testSinclairStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION, inputT3Stack);
    }

    public void testPauliStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.PAULI_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.PAULI_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.PAULI_DECOMPOSITION, inputT3Stack);
    }

    public void testFreemanStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION, inputT3Stack);
    }

    public void testCloudeStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION, inputT3Stack);
    }

    public void testHAAlphaStack() throws Exception {

        PolarimetricDecompositionOp op;

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setHAAlphaParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION, inputQuadFullStack);

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setHAAlphaParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION, inputC3Stack);

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setHAAlphaParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION, inputT3Stack);
    }

    public void testTouziStack() throws Exception {

        PolarimetricDecompositionOp op;

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setTouziParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.TOUZI_DECOMPOSITION, inputQuadFullStack);

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setTouziParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.TOUZI_DECOMPOSITION, inputC3Stack);

        op = (PolarimetricDecompositionOp)spi.createOperator();
        op.setTouziParameters(true, true, true, true);
        runDecomposition(op, PolarimetricDecompositionOp.TOUZI_DECOMPOSITION, inputT3Stack);
    }

    public void testVanZylStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.VANZYL_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.VANZYL_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.VANZYL_DECOMPOSITION, inputT3Stack);
    }

    public void testYamaguchiStack() throws Exception {

        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION, inputQuadFullStack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION, inputC3Stack);
        runDecomposition((PolarimetricDecompositionOp)spi.createOperator(),
                PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION, inputT3Stack);
    }
}
