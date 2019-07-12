/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.classification.gpf;

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
 * Unit test for PolarimetricClassification.
 */
public class TestClassifcationOp {

    private static File inputQuadFile;
    private static File inputQuadFullStackFile;
    private static File inputC3StackFile;
    private static File inputT3StackFile;

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        inputQuadFile = TestData.inputQuad;
        assumeTrue(inputQuadFile + " not found", inputQuadFile.exists());

        inputQuadFullStackFile = TestData.inputQuadFullStack;
        assumeTrue(inputQuadFullStackFile + " not found", inputQuadFullStackFile.exists());

        inputC3StackFile = TestData.inputC3Stack;
        assumeTrue(inputC3StackFile + " not found", inputC3StackFile.exists());

        inputT3StackFile = TestData.inputT3Stack;
        assumeTrue(inputT3StackFile + " not found", inputT3StackFile.exists());
    }

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new PolarimetricClassificationOp.Spi();

    private Product runClassification(final PolarimetricClassificationOp op, final String classifier,
                                      final File inputFile) throws Exception {
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetClassification(classifier);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);
        return targetProduct;
    }

    @Test
    public void testCloudePottierClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputQuadFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputQuadFullStackFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputC3StackFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, inputT3StackFile);
    }

    @Test
    public void testWishartClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, inputQuadFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, inputQuadFullStackFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, inputC3StackFile);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, inputT3StackFile);
    }

    @Test
    public void testTerrainClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION, inputQuadFile);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputQuadFullStackFile);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputC3StackFile);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, inputT3StackFile);
    }
}
