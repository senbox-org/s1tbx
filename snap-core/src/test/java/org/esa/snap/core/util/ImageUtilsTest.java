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

package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;


public class ImageUtilsTest {

    @Test
    public void testCreateRenderedImage() {
        testByte();
        testUShort();
        testShort();
        testInt();
        testFloat();
       testDouble();
    }

    private void testByte() {
        final RenderedImage image = createImage(ProductData.TYPE_UINT8, DataBuffer.TYPE_BYTE,
                                                new byte[]{12, 23, 34, 45});
        assertEquals(12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }


    private void testUShort() {
        final RenderedImage image = createImage(ProductData.TYPE_UINT16, DataBuffer.TYPE_USHORT,
                                                new short[]{12, 23, 34, 45});
        assertEquals(12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }

    private void testShort() {
        final RenderedImage image = createImage(ProductData.TYPE_INT16, DataBuffer.TYPE_SHORT,
                                                new short[]{-12, 23, 34, 45});
        assertEquals(-12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }

    private void testInt() {
        final RenderedImage image = createImage(ProductData.TYPE_INT32, DataBuffer.TYPE_INT,
                                                new int[]{-12, 23, 34, 45});
        assertEquals(-12, image.getData().getSample(0, 0, 0));
        assertEquals(23, image.getData().getSample(1, 0, 0));
        assertEquals(34, image.getData().getSample(0, 1, 0));
        assertEquals(45, image.getData().getSample(1, 1, 0));
    }


    private void testFloat() {
        final RenderedImage image = createImage(ProductData.TYPE_FLOAT32, DataBuffer.TYPE_FLOAT,
                                                new float[]{1.2f, 5.6f, -12.6f, -12345.6f});
        assertEquals(1.2f, image.getData().getSampleFloat(0, 0, 0), 1e-6);
        assertEquals(5.6f, image.getData().getSampleFloat(1, 0, 0), 1e-6);
        assertEquals(-12.6f, image.getData().getSampleFloat(0, 1, 0), 1e-6);
        assertEquals(-12345.6f, image.getData().getSampleFloat(1, 1, 0), 1e-6);
    }

    private void testDouble() {
        final RenderedImage image = createImage(ProductData.TYPE_FLOAT64, DataBuffer.TYPE_DOUBLE,
                                                new double []{1.2, 5.6, -12.6, -12345.6});
        assertEquals(1.2, image.getData().getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(5.6, image.getData().getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(-12.6, image.getData().getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(-12345.6, image.getData().getSampleDouble(1, 1, 0), 1e-6);
    }

    private RenderedImage createImage(int pdType, int dbType, Object data) {
        final ProductData floatData = ProductData.createInstance(pdType, data);
        final RenderedImage image = ImageUtils.createRenderedImage(2, 2, floatData);
        assertEquals(dbType, image.getSampleModel().getDataType());
        return image;
    }
}
