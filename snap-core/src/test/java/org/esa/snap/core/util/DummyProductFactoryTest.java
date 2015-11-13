package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DummyProductFactory.GC;
import org.esa.snap.core.util.DummyProductFactory.GP;
import org.esa.snap.core.util.DummyProductFactory.Occurrence;
import org.esa.snap.core.util.DummyProductFactory.Size;
import org.junit.Test;

import java.awt.Dimension;

import static org.esa.snap.core.util.DummyProductFactory.createProduct;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman
 */
public class DummyProductFactoryTest {

    @Test
    public void testSingleSizeCrsGeoCoding() throws Exception {
        Product product = createProduct(new DummyProductFactory.Type(Size.S, Occurrence.S, GC.MAP, Occurrence.S, GP.NMER));

        assertEquals("Test-SZ_S_S-GC_MAP_S_NMER", product.getName());
        assertEquals(new Dimension(120, 240), product.getSceneRasterSize());
        assertNotNull(product.getSceneGeoCoding());
        assertEquals(product.getSceneGeoCoding().getMapCRS(), product.getSceneCRS());
        assertArrayEquals(new String[]{
                "band_a",
                "band_b",
                "band_c",
        }, product.getBandGroup().getNodeNames());

        assertArrayEquals(new String[]{
                "mask_a",
                "mask_b",
                "mask_c",
        }, product.getMaskGroup().getNodeNames());
    }

    @Test
    public void testMultiSizeCrsGeoCoding() throws Exception {
        Product product = createProduct(new DummyProductFactory.Type(Size.S, Occurrence.M, GC.MAP, Occurrence.S, GP.NMER));

        assertEquals("Test-SZ_S_M-GC_MAP_S_NMER", product.getName());
        assertEquals(new Dimension(120, 240), product.getSceneRasterSize());
        assertNotNull(product.getSceneGeoCoding());
        assertEquals(product.getSceneGeoCoding().getMapCRS(), product.getSceneCRS());
        assertArrayEquals(new String[]{
                "band_a_1",
                "band_a_2",
                "band_a_6",
                "band_b_1",
                "band_b_2",
                "band_b_6",
                "band_c_1",
                "band_c_2",
                "band_c_6",
        }, product.getBandGroup().getNodeNames());

        assertArrayEquals(new String[]{
                "mask_a_1",
                "mask_a_2",
                "mask_a_6",
                "mask_b_1",
                "mask_b_2",
                "mask_b_6",
                "mask_c_1",
                "mask_c_2",
                "mask_c_6",
        }, product.getMaskGroup().getNodeNames());
    }
}
