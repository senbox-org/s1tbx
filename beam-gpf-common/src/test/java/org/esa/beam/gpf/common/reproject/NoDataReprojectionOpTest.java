package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class NoDataReprojectionOpTest extends AbstractReprojectionOpTest {

    @Test
    public void testNoDataIsPreservedFloat() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setNoDataValue(299);
        band.setNoDataValueUsed(true);
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, 299.0, 299.0);
    }

    @Test
    public void testNoDataIsPreservedFloat_withExpression() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setNoDataValue(299);
        band.setNoDataValueUsed(true);
        band.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, 299.0, 299.0);
    }

    @Test
    public void testNoDataIsReplaced_WithNaN() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, Float.NaN, Float.NaN);
    }

    @Test
    public void testNoDataParameter_WithExpressionAndValue() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setNoDataValue(299);
        band.setNoDataValueUsed(true);
        band.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, 42.0, 42.0);
    }

    @Test
    public void testNoDataParameter_WithExpression() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setValidPixelExpression("fneq("+FLOAT_BAND_NAME + ",299)");
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, 42.0, 42.0);

    }

    @Test
    public void testNoDataParameter_WithValue() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Band band = sourceProduct.getBand(FLOAT_BAND_NAME);
        band.setNoDataValue(299);
        band.setNoDataValueUsed(true);
        final Product targetPoduct = createReprojectedProduct();

        assertNoDataValue(targetPoduct, 42.0, 42.0);
    }

    @Test
    public void testNoDataParameter() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("noDataValue", 42.0);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNoDataValue(targetPoduct, 42.0, 299.0);
    }

    private void assertNoDataValue(Product targetPoduct, double noDataValue, double referenceValue) throws IOException {
        final Band targetBand = targetPoduct.getBand(FLOAT_BAND_NAME);
        assertEquals(noDataValue, targetBand.getNoDataValue(), EPS);
        assertTrue(targetBand.isNoDataValueUsed());
        assertNull(targetBand.getValidPixelExpression());
        testPixelValueFloat(targetPoduct, 23.5f, 13.5f, referenceValue, EPS);
    }


}
