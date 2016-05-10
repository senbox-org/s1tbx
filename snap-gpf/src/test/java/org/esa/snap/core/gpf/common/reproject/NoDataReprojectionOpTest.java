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

package org.esa.snap.core.gpf.common.reproject;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class NoDataReprojectionOpTest extends AbstractReprojectionOpTest {

    @Test
    public void testNoDataIsPreservedFloat() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        int xIndex = 23;
        int yIndex = 13;
        int dataValue = xIndex * yIndex;
        srcBand.setNoDataValue(dataValue);
        srcBand.setNoDataValueUsed(true);
        final Product targetProduct = createReprojectedProduct();

        assertNoDataValue(targetProduct.getBand(FLOAT_BAND_NAME), new PixelPos(xIndex + 0.5f, yIndex + 0.5f), dataValue, 299.0);
    }

    @Test
    public void testNoDataIsPreservedFloat_withExpression() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        srcBand.setNoDataValue(299);
        srcBand.setNoDataValueUsed(true);
        srcBand.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), 299.0, 299.0);
    }

    @Test
    public void testNoDataIsReplaced_WithNaN() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        srcBand.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), Float.NaN, Float.NaN);
    }

    @Test
    public void testNoDataParameter_WithExpressionAndValue() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        srcBand.setNoDataValue(299);
        srcBand.setNoDataValueUsed(true);
        srcBand.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), 42.0, 42.0);
    }

    @Test
    public void testNoDataParameter_WithExpression() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        srcBand.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), 42.0, 42.0);

    }

    @Test
    public void testNoDataParameter_WithValue() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band srcBand = sourceProduct.getBand(FLOAT_BAND_NAME);
        srcBand.setNoDataValue(299);
        srcBand.setNoDataValueUsed(true);
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), 42.0, 42.0);
    }

    @Test
    public void testNoDataParameter_Float() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct.getBand(FLOAT_BAND_NAME), new PixelPos(23.5f, 13.5f), 42.0, 299.0);
    }

    @Test
    public void testNoDataParameter_Int() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        parameterMap.put("noDataValue", 100.0);
        final Product targetPoduct = createReprojectedProduct();
        // int band has no no-data value or expression set
        // the noDataValue parameter has no effect
        // so we expect the same value as in the source product
        assertNoDataValue(targetPoduct.getBand(INT_BAND_NAME), new PixelPos(23.5f, 13.5f), 100.0, 299);
    }

    @Test
    public void testNoDataIfBandNoDataIsGiven_Int() throws IOException {
        parameterMap.put("crs", UTM33N_CODE);
        final Band srcBand = sourceProduct.getBand(INT_BAND_NAME);
        srcBand.setNoDataValue(299);
        srcBand.setNoDataValueUsed(true);

        final Product targetPoduct = createReprojectedProduct();
        assertNoDataValue(targetPoduct.getBand(INT_BAND_NAME), new PixelPos(23.5f, 13.5f), 299.0, 299);
    }

    @Test
    public void testNoDataIfExpressionIsGiven_Int() throws IOException {
        parameterMap.put("crs", WGS84_CODE);
        final Band srcBand = sourceProduct.getBand(INT_BAND_NAME);
        srcBand.setValidPixelExpression("(X-0.5) != 4");

        final Product targetPoduct = createReprojectedProduct();
        final Band targetBand = targetPoduct.getBand(INT_BAND_NAME);
        assertNoDataValue(targetBand, new PixelPos(4.5f, 0.5f), 0, 0.0);
        assertNoDataValue(targetBand, new PixelPos(4.5f, 1.5f), 0, 0.0);
        assertNoDataValue(targetBand, new PixelPos(4.5f, 18.5f), 0, 0.0);
        assertNoDataValue(targetBand, new PixelPos(5.5f, 18.5f), 0, 90.0);
    }

    private void assertNoDataValue(Band targetBand, final PixelPos sourcePixelPos,
                                   double noDataValue, double expectedValue) throws IOException {
        assertEquals(noDataValue, targetBand.getNoDataValue(), EPS);
        assertTrue(targetBand.isNoDataValueUsed());
        assertNull(targetBand.getValidPixelExpression());
        assertPixelValue(targetBand, sourcePixelPos.x, sourcePixelPos.y, expectedValue, EPS);
        boolean expectedValidState = !Double.isNaN(noDataValue) && noDataValue != expectedValue;
        assertPixelValidState(targetBand, sourcePixelPos.x, sourcePixelPos.y, expectedValidState);
        // upper left pixel has no source pixel -> should be no-data
        PixelPos upperLeft = new PixelPos(0.5f, 0.5f);
        assertTargetPixelValue(targetBand, upperLeft, noDataValue, EPS);
        assertTargetPixelValidState(targetBand, upperLeft, false);
    }


}
