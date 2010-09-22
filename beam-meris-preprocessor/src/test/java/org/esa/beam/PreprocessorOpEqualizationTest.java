/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.util.math.RsMathUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.esa.beam.dataio.envisat.EnvisatConstants.*;

public class PreprocessorOpEqualizationTest {

    private static final String RR_TEST_INPUT_RAD = "MER_RR__1PNMAP20090112_TestInput_Rad.dim";
    private static final String RR_TEST_EXPECTED = "MER_RR__1PNMAP20090112_ExpectedOutput.dim";
    private static final String FR_TEST_INPUT_RAD = "MER_FRS_1PNMAP20051130_TestInput_Rad.dim";
    private static final String FR_TEST_EXPECTED = "MER_FRS_1PNMAP20051130_ExpectedOutput.dim";

    private static PreprocessorOp.Spi preprocessorOpSpi = new PreprocessorOp.Spi();
    private static Product radianceSourceProductRR;
    private static Product expectedProductRR;
    private static Product radianceSourceProductFR;
    private static Product expectedProductFR;
    private static Map<String, Object> opParams;

    @BeforeClass
    public static void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(preprocessorOpSpi);
        opParams = new HashMap<String, Object>();
        opParams.put("doSmile", true);
        opParams.put("doEqualization", true);
        opParams.put("doRadToRefl", true);
        opParams.put("doRadiometricRecalibration", false);
        radianceSourceProductRR = readProduct(RR_TEST_INPUT_RAD);
        expectedProductRR = readProduct(RR_TEST_EXPECTED);
        radianceSourceProductFR = readProduct(FR_TEST_INPUT_RAD);
        expectedProductFR = readProduct(FR_TEST_EXPECTED);
    }


    @AfterClass
    public static void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(preprocessorOpSpi);
        radianceSourceProductRR.dispose();
        expectedProductRR.dispose();
        radianceSourceProductFR.dispose();
        expectedProductFR.dispose();
    }

    @Test
    public void testComputation_RR() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(PreprocessorOp.class);
        final Product product = GPF.createProduct(operatorName, opParams, radianceSourceProductRR);

        for (int i = 0; i < product.getBands().length - 2; i++) {
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 53, 4, false);
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 928, 3, false);
        }
    }

    @Test
    public void testComputation_FR() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(PreprocessorOp.class);
        final Product product = GPF.createProduct(operatorName, opParams, radianceSourceProductFR);

        for (int i = 0; i < product.getBands().length - 2; i++) {
            // currently only ensure that band 11 is not corrected by algorithm
            // no coefficients are provided for this band, but the expected product is corrected
            if (i == 10) {
                comparePixels(product.getBandAt(i), radianceSourceProductFR.getBandAt(i), 53, 4, true);
                comparePixels(product.getBandAt(i), radianceSourceProductFR.getBandAt(i), 928, 3, true);
                comparePixels(product.getBandAt(i), radianceSourceProductFR.getBandAt(i), 2264, 3, true);
                comparePixels(product.getBandAt(i), radianceSourceProductFR.getBandAt(i), 4012, 3, true);
            } else {
                comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 53, 4, false);
                comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 928, 3, false);
                comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 2264, 3, false);
                comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 4012, 3, false);
            }
        }
    }

    private static void comparePixels(Band actualBand, Band expectedBand, int x, int y, boolean convertExpectedToRefl) {
        float expectedValue = expectedBand.getGeophysicalImage().getData().getSampleFloat(x, y, 0);
        final float actualValue = actualBand.getGeophysicalImage().getData().getSampleFloat(x, y, 0);
        if (convertExpectedToRefl) {
            final TiePointGrid szaGrid = expectedBand.getProduct().getTiePointGrid(MERIS_SUN_ZENITH_DS_NAME);
            final float sza = szaGrid.getGeophysicalImage().getData().getSampleFloat(x, y, 0);
            expectedValue = RsMathUtils.radianceToReflectance(expectedValue, sza, expectedBand.getSolarFlux());
        }
        final String message = String.format("Error comparing pixel (%d, %d) of bands '%s' and '%s':\n", x, y,
                                             actualBand.getName(), expectedBand.getName());
        assertEquals(message, expectedValue, actualValue, 1.0e-3);
    }


    private static Product readProduct(String s) throws IOException {
        final URL radianceProductUrl = PreprocessorOpEqualizationTest.class.getResource(s);
        return ProductIO.readProduct(radianceProductUrl.getFile());
    }

}
