package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.LineTimeCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Tonio Fincke
 */
public class ResamplingOpTest {

    @Test
    public void testTimeInformationIsPreserved() {
        final Product product = new Product("name", "test", 2, 2);
        Date date = new Date();
        ProductData.UTC startTime = ProductData.UTC.create(new Date(date.getTime() - 5000), 0);
        ProductData.UTC endTime = ProductData.UTC.create(new Date(date.getTime()), 0);
        product.setSceneTimeCoding(new LineTimeCoding(2, startTime.getMJD(), endTime.getMJD()));

        product.setStartTime(startTime);
        product.setEndTime(endTime);
        product.addBand("band_1", "X + Y");
        product.addBand("band_2", "X + 1 + Y");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("referenceBandName", "band_1");
        Product resampledProduct = GPF.createProduct("Resample", parameters, product);
        assertEquals(startTime.getAsDate().getTime(), resampledProduct.getStartTime().getAsDate().getTime());
        assertEquals(endTime.getAsDate().getTime(), resampledProduct.getEndTime().getAsDate().getTime());
        assertNotNull(resampledProduct.getSceneTimeCoding());
        assertEquals(endTime.getMJD(), resampledProduct.getSceneTimeCoding().getMJD(new PixelPos(0, 1)));
    }

    @Test
    public void testAllNodesHaveIdentitySceneTransform() {
        final Product product = new Product("name", "tapce", 2, 2);
        product.addBand("band_1", "X + Y");
        final Band band2 = product.addBand("band_2", "X + 1 + Y");

        assertTrue(ResamplingOp.allNodesHaveIdentitySceneTransform(product));

        band2.setModelToSceneTransform(MathTransform2D.NULL);

        assertFalse(ResamplingOp.allNodesHaveIdentitySceneTransform(product));
    }

    @Test
    public void testOnlyReferenceBandIsSet() {
        Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("referenceBandName", "dummy");
        parameterMap.put("targetWidth", 3);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If referenceBandName is set, targetWidth, targetHeight, and targetResolution must not be set",
                         oe.getMessage());
        }
        parameterMap.remove("targetWidth");
        parameterMap.put("targetHeight", 3);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If referenceBandName is set, targetWidth, targetHeight, and targetResolution must not be set",
                         oe.getMessage());
        }
        parameterMap.remove("targetHeight");
        parameterMap.put("targetResolution", 20);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If referenceBandName is set, targetWidth, targetHeight, and targetResolution must not be set",
                         oe.getMessage());
        }
    }

    @Test
    public void testOnlyTargetWidthAndHeightAreSet() {
        Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("targetWidth", 3);
        parameterMap.put("targetHeight", 3);
        parameterMap.put("referenceBandName", "dummy");
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If referenceBandName is set, targetWidth, targetHeight, and targetResolution must not be set",
                         oe.getMessage());
        }
        parameterMap.remove("referenceBandName");
        parameterMap.put("targetResolution", 20);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If targetResolution is set, targetWidth, targetHeight, and referenceBandName must not be set",
                         oe.getMessage());
        }
    }

    @Test
    public void testBothTargetWidthAndHeightAreSet() {
        Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("targetWidth", 3);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If targetWidth is set, targetHeight must be set, too.", oe.getMessage());
        }
        parameterMap.remove("targetWidth");
        parameterMap.put("targetHeight", 3);
        try {
            GPF.createProduct("Resample", parameterMap, product);
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("If targetHeight is set, targetWidth must be set, too.", oe.getMessage());
        }
    }
}