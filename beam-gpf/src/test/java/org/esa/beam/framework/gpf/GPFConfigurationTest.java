package org.esa.beam.framework.gpf;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.OpImage;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

public class GPFConfigurationTest {

    private String oldValue;

    @Before
    public void setUp() throws Exception {
        oldValue = System.getProperty(GPF.DISABLE_TILE_CACHE_PROPERTY);
    }

    @After
    public void tearDown() throws Exception {
        if (oldValue != null) {
            System.setProperty(GPF.DISABLE_TILE_CACHE_PROPERTY, oldValue);
        } else {
            System.clearProperty(GPF.DISABLE_TILE_CACHE_PROPERTY);
        }
    }

    @Test
    public void testThatTileCacheCanBeDisabled() {
        System.setProperty(GPF.DISABLE_TILE_CACHE_PROPERTY, "true");
        assertNull(createOpImage().getTileCache());
    }

    @Test
    public void testThatTileCacheCanBeEnabled() {
        System.setProperty(GPF.DISABLE_TILE_CACHE_PROPERTY, "false");
        assertNotNull(createOpImage().getTileCache());
    }

    private OpImage createOpImage() {
        SubsetOp subsetOp = new SubsetOp();
        Product sourceProduct = new Product("name", "type", 16, 16);
        sourceProduct.addBand("x", ProductData.TYPE_INT32);
        subsetOp.setSourceProduct(sourceProduct);
        Product targetProduct = subsetOp.getTargetProduct();
        RenderedImage image = targetProduct.getBand("x").getSourceImage().getImage(0);
        assertTrue(image instanceof OpImage);
        return (OpImage) image;
    }
}
