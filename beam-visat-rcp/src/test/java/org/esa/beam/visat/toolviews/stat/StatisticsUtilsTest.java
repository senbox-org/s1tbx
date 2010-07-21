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
