package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TransectProfileData;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class StatisticsUtilsTest {

    @Test
    public void testCreateTransectProfileText_Byte() throws IOException {
        final Product product = new Product("X", "Y", 2, 1);
        Band node = product.addBand("name", ProductData.TYPE_INT8);
        node.setSynthetic(true);
        node.setNoDataValue(0);
        node.setNoDataValueUsed(true);
        final byte[] data = new byte[product.getSceneRasterWidth() * product.getSceneRasterHeight()];
        Arrays.fill(data, (byte) 1);
        data[0] = 0; // no data
        node.setData(ProductData.createInstance(data));

        final Line2D.Float shape = new Line2D.Float(0.5f, 0.5f, 1.5f, 0.5f);
        final TransectProfileData profileData = node.createTransectProfileData(shape);
        final String profileDataString = StatisticsUtils.TransectProfile.createTransectProfileText(node, profileData);
        assertTrue(profileDataString.contains("NaN"));
        assertFalse(profileDataString.toLowerCase().contains("no data"));
    }

    @Test
    public void testCreateTransectProfileText_Float() throws IOException {
        final Product product = new Product("X", "Y", 2, 1);
        Band node = product.addBand("name", ProductData.TYPE_FLOAT32);
        node.setSynthetic(true);
        final float[] data = new float[product.getSceneRasterWidth() * product.getSceneRasterHeight()];
        Arrays.fill(data, 1.0f);
        data[0] = Float.NaN; // no data
        node.setData(ProductData.createInstance(data));

        final Line2D.Float shape = new Line2D.Float(0.5f, 0.5f, 1.5f, 0.5f);
        final TransectProfileData profileData = node.createTransectProfileData(shape);
        final String profileDataString = StatisticsUtils.TransectProfile.createTransectProfileText(node, profileData);
        assertTrue(profileDataString.contains("NaN"));
        assertFalse(profileDataString.toLowerCase().contains("no data"));
    }
}
