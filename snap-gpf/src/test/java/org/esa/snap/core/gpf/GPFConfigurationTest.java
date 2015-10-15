package org.esa.snap.core.gpf;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.runtime.Config;
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
        oldValue = Config.instance().preferences().get(GPF.DISABLE_TILE_CACHE_PROPERTY, null);
    }

    @After
    public void tearDown() throws Exception {
        if (oldValue != null) {
            Config.instance().preferences().put(GPF.DISABLE_TILE_CACHE_PROPERTY, oldValue);
        } else {
            Config.instance().preferences().remove(GPF.DISABLE_TILE_CACHE_PROPERTY);
        }
    }

    @Test
    public void testThatTileCacheCanBeDisabled() {
        Config.instance().preferences().put(GPF.DISABLE_TILE_CACHE_PROPERTY, "true");
        assertNull(createOpImage().getTileCache());
    }

    @Test
    public void testThatTileCacheCanBeEnabled() {
        Config.instance().preferences().put(GPF.DISABLE_TILE_CACHE_PROPERTY, "false");
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
