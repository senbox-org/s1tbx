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
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;

public class EqualizationOpTest {
    private static final String RR_TEST_INPUT_RAD = "MER_RR__1PNMAP20090112_TestInput_Rad.dim";
    private static final String RR_TEST_INPUT_RAD_SMILE = "MER_RR__1PNMAP20090112_TestInput_Rad_Smile.dim";
    private static final String RR_TEST_EXPECTED = "MER_RR__1PNMAP20090112_ExpectedOutput.dim";
    private static final String FR_TEST_INPUT_RAD = "MER_FRS_1PNMAP20051130_TestInput_Rad.dim";
    private static final String FR_TEST_INPUT_RAD_SMILE = "MER_FRS_1PNMAP20051130_TestInput_Rad_Smile.dim";
    private static final String FR_TEST_EXPECTED = "MER_FRS_1PNMAP20051130_ExpectedOutput.dim";

    private static EqualizationOp.Spi equalizationOpSpi = new EqualizationOp.Spi();
    private static Product radianceSourceProductRR;
    private static Product smileSourceProductRR;
    private static Product expectedProductRR;
    private static Product radianceSourceProductFR;
    private static Product smileSourceProductFR;
    private static Product expectedProductFR;

    @BeforeClass
    public static void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(equalizationOpSpi);
        radianceSourceProductRR = readProduct(RR_TEST_INPUT_RAD);
        smileSourceProductRR = readProduct(RR_TEST_INPUT_RAD_SMILE);
        expectedProductRR = readProduct(RR_TEST_EXPECTED);
        radianceSourceProductFR = readProduct(FR_TEST_INPUT_RAD);
        smileSourceProductFR = readProduct(FR_TEST_INPUT_RAD_SMILE);
        expectedProductFR = readProduct(FR_TEST_EXPECTED);
        // todo - test product has wrong version number
        // remove the following when clarified
        adaptVersionToReprocessingTwo(radianceSourceProductFR);
        adaptVersionToReprocessingTwo(smileSourceProductFR);
    }


    @AfterClass
    public static void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(equalizationOpSpi);
        radianceSourceProductRR.dispose();
        smileSourceProductRR.dispose();
        expectedProductRR.dispose();
        radianceSourceProductFR.dispose();
        smileSourceProductFR.dispose();
        expectedProductFR.dispose();
    }

    @Test
    public void testComputation_RR_WithDefaultParameter() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Product product = GPF.createProduct(operatorName, GPF.NO_PARAMS,
                                                        radianceSourceProductRR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 928, 3);
        }
    }

    @Test
    public void testComputation_FR_WithDefaultParameter() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Product product = GPF.createProduct(operatorName, GPF.NO_PARAMS,
                                                        radianceSourceProductFR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            // currently skip band 11
            // no coefficients are provided for this band
            if(i == 10) {
                continue;
            }
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 928, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 2264, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 4012, 3);
        }
    }

    @Test
    public void testComputation_RR_WithSmileOn() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", true);
        final Product product = GPF.createProduct(operatorName, parameterMap,
                                                        radianceSourceProductRR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 928, 3);
        }
    }

    @Test
    public void testComputation_FR_WithSmileOn() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", true);
        final Product product = GPF.createProduct(operatorName, parameterMap,
                                                        radianceSourceProductFR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            // currently skip band 11
            // no coefficients are provided for this band
            if(i == 10) {
                continue;
            }
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 928, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 2264, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 4012, 3);
        }
    }


    @Test
    public void testComputation_RR_WithSmileOff() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", false);
        final Product product = GPF.createProduct(operatorName, parameterMap,
                                                        smileSourceProductRR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductRR.getBandAt(i), 928, 3);
        }
    }

    @Test
    public void testComputation_FR_WithSmileOff() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", false);
        final Product product = GPF.createProduct(operatorName, parameterMap,
                                                        smileSourceProductFR);

        for (int i= 0; i < product.getBands().length - 2; i++) {
            // currently skip band 11
            // no coefficients are provided for this band
            if(i == 10) {
                continue;
            }
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 53, 4);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 928, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 2264, 3);
            comparePixels(product.getBandAt(i), expectedProductFR.getBandAt(i), 4012, 3);
        }
    }

    @Test(expected = OperatorException.class)
    public void testParseReproVersion_MERIS_Fails() {
        EqualizationOp.parseReprocessingVersion("MERIS", 4.67f);
    }

    @Test(expected = OperatorException.class)
    public void testParseReproVersion_MEGS_Fails() {
        EqualizationOp.parseReprocessingVersion("MEGS-PC", 8.1f);
    }

    @Test
    public void testParseReproVersion() {
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.02f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.03f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.04f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MERIS", 5.05f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MEGS-PC", 7.4f));
        assertEquals(2, EqualizationOp.parseReprocessingVersion("MEGS-PC", 7.41f));

        assertEquals(3, EqualizationOp.parseReprocessingVersion("MEGS-PC", 8.0f));
    }

    @Test
    public void testToJulianDay() {
        assertEquals(2455414, (long) JulianDate.julianDate(2010, 7, 6));
        assertEquals(2452365, (long) JulianDate.julianDate(2002, 3, 1));
    }

    private static void comparePixels(Band actualBand, Band expectedBand, int x, int y) {
        final double expectedValue = expectedBand.getGeophysicalImage().getData().getSampleDouble(x, y, 0);
        final double targetValue = actualBand.getGeophysicalImage().getData().getSampleDouble(x, y, 0);
        final String message = String.format("Error comparing pixel (%d, %d) of bands '%s' and '%s':\n", x, y,
                                             actualBand.getName(), expectedBand.getName());
        assertEquals(message, expectedValue, targetValue, 1.0e-3);
    }


    private static Product readProduct(String s) throws IOException {
        final URL radianceProductUrl = EqualizationOpTest.class.getResource(s);
        return ProductIO.readProduct(radianceProductUrl.getFile());
    }

    private static void adaptVersionToReprocessingTwo(Product product) {
        final MetadataElement metadata = product.getMetadataRoot();
        metadata.getElement("MPH").setAttributeString("SOFTWARE_VER", "MERIS/5.02");
    }

}
