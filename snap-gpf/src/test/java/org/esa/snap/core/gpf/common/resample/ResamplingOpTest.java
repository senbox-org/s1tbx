package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Tonio Fincke
 */
public class ResamplingOpTest {

    @Test
    public void testAllNodesHaveIdentitySceneTransform() {
        final Product product = new Product("name", "tapce", 2, 2);
        product.addBand("band_1", "X + Y");
        final Band band2 = product.addBand("band_2", "X + 1 + Y");

        assertTrue(ResamplingOp_Old.allNodesHaveIdentitySceneTransform(product));

        band2.setModelToSceneTransform(MathTransform2D.NULL);

        assertFalse(ResamplingOp_Old.allNodesHaveIdentitySceneTransform(product));
    }

    @Test
    public void testOnlyReferenceBandIsSet() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new ResamplingOp.Spi());
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
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new ResamplingOp.Spi());
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
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new ResamplingOp.Spi());
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