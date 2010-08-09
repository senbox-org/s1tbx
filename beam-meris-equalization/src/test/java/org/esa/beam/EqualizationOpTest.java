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
    private static final String RADIANCE_TEST_INPUT = "MER_RR__1PPBCM20090112_ValidationInput_radiance.dim";
    private static final String SMILE_TEST_INPUT = "MER_RR__1PPBCM20090112_ValidationInput_smile.dim";
    private static final String EXPECTED_OUTPUT = "MER_RR__1PPBCM20090112_ValidationOutput.dim";

    private static EqualizationOp.Spi equalizationOpSpi = new EqualizationOp.Spi();
    private static Product radianceSourceProduct;
    private static Product smileSourceProduct;
    private static Product expectedProduct;

    @BeforeClass
    public static void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(equalizationOpSpi);
        final URL radianceProductUrl = EqualizationOpTest.class.getResource(RADIANCE_TEST_INPUT);
        radianceSourceProduct = ProductIO.readProduct(radianceProductUrl.getFile());
        final URL smileProductUrl = EqualizationOpTest.class.getResource(SMILE_TEST_INPUT);
        smileSourceProduct = ProductIO.readProduct(smileProductUrl.getFile());
        final URL expectedProductUrl = EqualizationOpTest.class.getResource(EXPECTED_OUTPUT);
        expectedProduct = ProductIO.readProduct(expectedProductUrl.getFile());

    }

    @AfterClass
    public static void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(equalizationOpSpi);
    }

    @Test
    public void testComputation_WithDefaultParameter() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Product targetProduct = GPF.createProduct(operatorName, GPF.NO_PARAMS,
                                                        radianceSourceProduct);

        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 10, 0);
        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 20, 0);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 3);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 2);

    }

    @Test
    public void testComputation_WithSmileOn() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", true);
        final Product targetProduct = GPF.createProduct(operatorName, parameterMap,
                                                        radianceSourceProduct);

        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 10, 0);
        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 20, 0);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 3);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 2);
    }

    @Test
    public void testComputation_WithSmileOff() throws URISyntaxException, IOException {
        String operatorName = OperatorSpi.getOperatorAlias(EqualizationOp.class);
        final Map<String,Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("doSmile", false);
        final Product targetProduct = GPF.createProduct(operatorName, parameterMap,
                                                        smileSourceProduct);

        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 10, 0);
        comparePixels(targetProduct.getBands()[0], expectedProduct.getBands()[0], 20, 0);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 3);
        comparePixels(targetProduct.getBands()[1], expectedProduct.getBands()[1], 10, 2);
    }

    private void comparePixels(Band band, Band band1, int x, int y) {
        final double expectedValue = band1.getGeophysicalImage().getData().getSampleFloat(x, y, 0);
        final double targetValue = band.getGeophysicalImage().getData().getSampleFloat(x, y, 0);
        assertEquals(expectedValue, targetValue, 1.0e-4);
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
}
