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

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.s1tbx.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit test for PolarimetricClassification.
 */
public class TestClassifcationOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new PolarimetricClassificationOp.Spi();

    private Product runClassification(final PolarimetricClassificationOp op, final String classifier,
                                      final File inputFile) throws Exception {
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return null;
        }
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
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, TestData.inputQuad);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, TestData.inputQuadFullStack);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, TestData.inputC3Stack);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, TestData.inputT3Stack);
    }

    @Test
    public void testWishartClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, TestData.inputQuad);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, TestData.inputQuadFullStack);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, TestData.inputC3Stack);
        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, TestData.inputT3Stack);
    }

    @Test
    public void testTerrainClassifier() throws Exception {

        runClassification((PolarimetricClassificationOp) spi.createOperator(),
                PolarimetricClassificationOp.UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION, TestData.inputQuad);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, TestData.inputQuadFullStack);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, TestData.inputC3Stack);
//        runClassification((PolarimetricClassificationOp)spi.createOperator(),
//                PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION, TestData.inputT3Stack);
    }
}
