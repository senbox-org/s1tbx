package org.esa.beam.framework.datamodel;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class RasterDataNodeTest {

    @Test
    public void testGetPixelString_Byte() {
        final Product product = new Product("X", "Y", 2, 1);
        Band node = product.addBand("name", ProductData.TYPE_INT8);
        node.setSynthetic(true);
        node.setNoDataValue(0);
        node.setNoDataValueUsed(true);
        final byte[] data = new byte[product.getSceneRasterWidth() * product.getSceneRasterHeight()];
        Arrays.fill(data, (byte) 1);
        data[0] = 0; // no data
        node.setData(ProductData.createInstance(data));

        assertEquals("NaN", node.getPixelString(0, 0));
        assertEquals("1", node.getPixelString(1, 0));
    }

    @Test
    public void testGetPixelString_Float() {
        final Product product = new Product("X", "Y", 2, 1);
        Band node = product.addBand("name", ProductData.TYPE_FLOAT32);
        node.setSynthetic(true);
        final float[] data = new float[product.getSceneRasterWidth() * product.getSceneRasterHeight()];
        Arrays.fill(data, 1.0f);
        data[0] = Float.NaN; // no data
        node.setData(ProductData.createInstance(data));

        assertEquals("NaN", node.getPixelString(0, 0));
        assertEquals("1.0", node.getPixelString(1, 0));
    }

    @Test
    public void testTimeCoding() throws Exception {
        final Band band = new Band("dummy", ProductData.TYPE_INT32, 10, 10);
        assertNull(band.getTimeCoding());
        final Product product = new Product("X", "Y", 10, 10);
        product.addBand(band);
        assertNull(product.getTimeCoding());
        product.setStartTime(ProductData.UTC.parse("15-06-2010", "dd-MM-yyyy"));
        assertNotNull(product.getTimeCoding());
        assertEquals(band.getTimeCoding(), product.getTimeCoding());
    }
}
