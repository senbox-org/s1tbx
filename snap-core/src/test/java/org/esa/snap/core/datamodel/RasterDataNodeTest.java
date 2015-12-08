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

package org.esa.snap.core.datamodel;

import com.bc.ceres.glevel.MultiLevelModel;
import org.junit.Assert;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class RasterDataNodeTest {

    /**
     * Tests use of Product.getNumResolutionsMax in RasterDataNode.getMultiLevelMode
     */
    @Test
    public void testGetMultiLevelModel() throws Exception {
        MultiLevelModel mlm1, mlm2;
        final Product p = new Product("P", "T", 10960, 10960);

        final Band b1 = p.addBand("B1", "0"); // Virtual band image --> source image set
        final Band b2 = p.addBand("B2", ProductData.TYPE_FLOAT32); // Normal band image --> source image NOT set

        mlm1 = b1.getMultiLevelModel();
        mlm2 = b2.getMultiLevelModel();
        assertEquals(0, p.getNumResolutionsMax());
        assertEquals(7, mlm1.getLevelCount());
        assertEquals(7, mlm2.getLevelCount());

        p.setNumResolutionsMax(3);

        b1.getSourceImage();

        mlm1 = b1.getMultiLevelModel();
        mlm2 = b2.getMultiLevelModel();
        assertEquals(3, p.getNumResolutionsMax());
        assertEquals(3, mlm1.getLevelCount());
        assertEquals(3, mlm2.getLevelCount());
    }

    @Test
    public void testImageToModelTransformCannotDetermine() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformSetterGetter() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        band.setImageToModelTransform(AffineTransform.getScaleInstance(.6, .3));
        assertEquals(AffineTransform.getScaleInstance(.6, .3), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformIsIdentity() throws Exception {
        Product product = new Product("N", "T", 4, 2);
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        product.addBand(band);
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testImageToModelTransformIsNewInstance() throws Exception {
        Product product = new Product("N", "T", 4, 2);
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        product.addBand(band);
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(2.0, 2.0);
        band.setImageToModelTransform(scaleInstance);
        assertEquals(scaleInstance, band.getImageToModelTransform());
        assertNotSame(scaleInstance, band.getImageToModelTransform());
        assertNotSame(band.getImageToModelTransform(), band.getImageToModelTransform());
        scaleInstance.rotate(0.1, 0.2);
        assertNotEquals(scaleInstance, band.getImageToModelTransform());
    }

    @Test(expected = IllegalStateException.class)
    public void testImageToModelTransformIsRuledBySourceImage() throws Exception {
        Band band = new Band("B", ProductData.TYPE_FLOAT32, 4, 2);
        band.setSourceImage(ConstantDescriptor.create(4f, 2f, new Float[]{0f}, null));
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
        band.setImageToModelTransform(AffineTransform.getScaleInstance(.6, .3));
        assertEquals(new AffineTransform(), band.getImageToModelTransform());
    }

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
    public void testGetPixelString() throws IOException {
        Locale.setDefault(Locale.UK);

        final Product product = new Product("X", "Y", 2, 1);
        Band b1 = product.addBand("b1", "X < 1 ? NaN : 1.0", ProductData.TYPE_FLOAT32);
        Band b2 = product.addBand("b2", "X < 1 ? NaN : 2.0", ProductData.TYPE_FLOAT64);
        Band b3 = product.addBand("b3", "X < 1 ? 0 : 3", ProductData.TYPE_UINT16);
        Band b4 = product.addBand("b4", "X < 1 ? 0 : 4", ProductData.TYPE_INT8);
        b1.loadRasterData();
        b2.loadRasterData();
        b3.loadRasterData();
        b4.loadRasterData();

        b3.setNoDataValue(0);
        b3.setNoDataValueUsed(true);

        b4.setNoDataValue(0);
        b4.setNoDataValueUsed(true);

        assertEquals("NaN", b1.getPixelString(0, 0));
        assertEquals("1.0", b1.getPixelString(1, 0));

        assertEquals("NaN", b2.getPixelString(0, 0));
        assertEquals("2.0", b2.getPixelString(1, 0));

        assertEquals("NaN", b3.getPixelString(0, 0));
        assertEquals("3", b3.getPixelString(1, 0));

        assertEquals("NaN", b4.getPixelString(0, 0));
        assertEquals("4", b4.getPixelString(1, 0));
    }

}
