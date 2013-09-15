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
 * Unit test for PolarimetricClassification.
 */
public class TestClassifcationOp extends TestCase {

    private OperatorSpi spi;

    private final static String inputPathQuad =          TestUtils.rootPathExpectedProducts+"\\input\\QuadPol\\QuadPol_subset_0_of_RS2-SLC-PDS_00058900.dim";
    private final static String inputQuadFullStack =     TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-Quad_Pol_Stack.dim";
    private final static String inputC3Stack =           TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-C3-Stack.dim";
    private final static String inputT3Stack =           TestUtils.rootPathExpectedProducts+"\\input\\QuadPolStack\\RS2-T3-Stack.dim";

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new PolarimetricClassificationOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    private static Product runClassification(final PolarimetricClassificationOp op, final String classifier,
                                             final String path) throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(path);

        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetClassification(classifier);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        return targetProduct;
    }

    public void testCloudePottierClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputPathQuad);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputQuadFullStack);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputC3Stack);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputT3Stack);
    }

    public void testWishartClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION, inputPathQuad);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION, inputQuadFullStack);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION, inputC3Stack);
        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION, inputT3Stack);
    }

    public void testTerrainClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp)spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputPathQuad);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputQuadFullStack);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputC3Stack);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputT3Stack);
    }
}