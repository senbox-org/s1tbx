package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DummyProductBuilder.GC;
import org.esa.snap.core.util.DummyProductBuilder.SizeOcc;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author Norman
 */
public class DummyProductBuilderTest {

    @Test
    public void testDefaults() throws Exception {
        Product product = new DummyProductBuilder().create();

        assertEquals("test_sz_S(1)_i2m_N_gc_0(U)_gp_M", product.getName());
        assertEquals("size:SMALL(SINGLE), i2m:NOT_SET, geo-coding:NONE(UNIQUE), geo-pos:NULL_MERIDIAN", product.getDescription());
    }

    @Test
    public void testNoneSizeMapGeoCoding() throws Exception {
        Product product = new DummyProductBuilder().sizeOcc(SizeOcc.NONE).gc(GC.MAP).create();

        assertEquals("test_sz_S(0)_i2m_N_gc_M(U)_gp_M", product.getName());
        assertEquals("size:SMALL(NONE), i2m:NOT_SET, geo-coding:MAP(UNIQUE), geo-pos:NULL_MERIDIAN", product.getDescription());
        assertEquals(new Dimension(120, 240), product.getSceneRasterSize());
        assertNotNull(product.getSceneGeoCoding());
        assertEquals(product.getSceneGeoCoding().getMapCRS(), product.getSceneCRS());
        assertArrayEquals(new String[0], product.getBandGroup().getNodeNames());
        assertArrayEquals(new String[0], product.getMaskGroup().getNodeNames());
        assertArrayEquals(new String[]{
                "tpgrid_a",
                "tpgrid_b"
        }, product.getTiePointGridGroup().getNodeNames());
    }

    @Test
    public void testSingleSizeMapGeoCoding() throws Exception {
        Product product = new DummyProductBuilder().gc(GC.MAP).create();

        assertEquals("test_sz_S(1)_i2m_N_gc_M(U)_gp_M", product.getName());
        assertEquals("size:SMALL(SINGLE), i2m:NOT_SET, geo-coding:MAP(UNIQUE), geo-pos:NULL_MERIDIAN", product.getDescription());
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

        assertArrayEquals(new String[]{
                "tpgrid_a",
                "tpgrid_b"
        }, product.getTiePointGridGroup().getNodeNames());
    }

    @Test
    public void testMultiSizeMapGeoCoding() throws Exception {
        Product product = new DummyProductBuilder().sizeOcc(SizeOcc.MULTI).gc(GC.MAP).create();

        assertEquals("test_sz_S(N)_i2m_N_gc_M(U)_gp_M", product.getName());
        assertEquals("size:SMALL(MULTI), i2m:NOT_SET, geo-coding:MAP(UNIQUE), geo-pos:NULL_MERIDIAN", product.getDescription());
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

        assertArrayEquals(new String[]{
                "tpgrid_a",
                "tpgrid_b"
        }, product.getTiePointGridGroup().getNodeNames());
    }
}
