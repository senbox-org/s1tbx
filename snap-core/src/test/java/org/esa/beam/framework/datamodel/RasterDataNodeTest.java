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

}
