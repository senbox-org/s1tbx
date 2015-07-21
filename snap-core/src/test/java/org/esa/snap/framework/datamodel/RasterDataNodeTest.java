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

package org.esa.snap.framework.datamodel;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
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
        assertEquals("1.00000", b1.getPixelString(1, 0));

        assertEquals("NaN", b2.getPixelString(0, 0));
        assertEquals("2.0000000000", b2.getPixelString(1, 0));

        assertEquals("NaN", b3.getPixelString(0, 0));
        assertEquals("3", b3.getPixelString(1, 0));

        assertEquals("NaN", b4.getPixelString(0, 0));
        assertEquals("4", b4.getPixelString(1, 0));
    }

}
