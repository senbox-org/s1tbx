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

package org.esa.snap.core.image;

import junit.framework.TestCase;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.ImageLayout;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

public class ImageHeaderTest extends TestCase {

    public void testLoadFloatImage() throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(
                new InputStreamReader(ImageHeaderTest.class.getResourceAsStream("float-image.properties")),
                null, null);
        assertEquals("raw", imageHeader.getTileFormat());
        final ImageLayout imageLayout = imageHeader.getImageLayout();
        assertNotNull(imageLayout);
        assertEquals(1, imageLayout.getMinX(null));
        assertEquals(-1, imageLayout.getMinY(null));
        assertEquals(1280, imageLayout.getWidth(null));
        assertEquals(1024, imageLayout.getHeight(null));
        assertEquals(0, imageLayout.getTileGridXOffset(null));
        assertEquals(6, imageLayout.getTileGridYOffset(null));
        assertEquals(512, imageLayout.getTileWidth(null));
        assertEquals(256, imageLayout.getTileHeight(null));
        assertNull(imageLayout.getColorModel(null));
        assertNotNull(imageLayout.getSampleModel(null));
        assertEquals(512, imageLayout.getSampleModel(null).getWidth());
        assertEquals(256, imageLayout.getSampleModel(null).getHeight());
        assertEquals(1, imageLayout.getSampleModel(null).getNumBands());
        assertEquals(4, imageLayout.getSampleModel(null).getDataType());
        assertEquals(32, imageLayout.getSampleModel(null).getSampleSize()[0]);
    }

    public void testLoadBitImage() throws IOException {
        final ImageHeader imageHeader = ImageHeader.load(
                new InputStreamReader(ImageHeaderTest.class.getResourceAsStream("bit-image.properties")),
                null, null);
        assertEquals("raw", imageHeader.getTileFormat());
        final ImageLayout imageLayout = imageHeader.getImageLayout();
        assertNotNull(imageLayout);
        assertEquals(1, imageLayout.getMinX(null));
        assertEquals(-1, imageLayout.getMinY(null));
        assertEquals(1280, imageLayout.getWidth(null));
        assertEquals(1024, imageLayout.getHeight(null));
        assertEquals(0, imageLayout.getTileGridXOffset(null));
        assertEquals(6, imageLayout.getTileGridYOffset(null));
        assertEquals(512, imageLayout.getTileWidth(null));
        assertEquals(256, imageLayout.getTileHeight(null));
        assertNull(imageLayout.getColorModel(null));
        assertNotNull(imageLayout.getSampleModel(null));
        assertEquals(512, imageLayout.getSampleModel(null).getWidth());
        assertEquals(256, imageLayout.getSampleModel(null).getHeight());
        assertEquals(1, imageLayout.getSampleModel(null).getNumBands());
        assertEquals(0, imageLayout.getSampleModel(null).getDataType());
        assertEquals(1, imageLayout.getSampleModel(null).getSampleSize()[0]);
    }

    public void testStoreAndLoad() throws IOException {
        int minX = 1;
        int minY = -1;
        int width = 1280;
        int height = 1024;
        int tileGridXOffset = 0;
        int tileGridYOffset = 6;
        int tileWidth = 512;
        int tileHeight = 256;
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_FLOAT, width, height);
        ColorModel colorModel = null;

        ImageLayout imageLayout = new ImageLayout(minX, minY,
                                                  width, height,
                                                  tileGridXOffset,
                                                  tileGridYOffset,
                                                  tileWidth, tileHeight,
                                                  sampleModel, colorModel);


        StringWriter writer = new StringWriter();

        ImageHeader imageHeader = new ImageHeader(imageLayout, "raw");

        imageHeader.store(writer, null);

        imageHeader = ImageHeader.load(new StringReader(writer.toString()), null, null);

        assertNotNull(imageHeader.getImageLayout());
        assertEquals(minX, imageHeader.getImageLayout().getMinX(null));
        assertEquals(minY, imageHeader.getImageLayout().getMinY(null));
        assertEquals(width, imageHeader.getImageLayout().getWidth(null));
        assertEquals(height, imageHeader.getImageLayout().getHeight(null));
        assertEquals(tileGridXOffset, imageHeader.getImageLayout().getTileGridXOffset(null));
        assertEquals(tileGridYOffset, imageHeader.getImageLayout().getTileGridYOffset(null));
        assertEquals(tileWidth, imageHeader.getImageLayout().getTileWidth(null));
        assertEquals(tileHeight, imageHeader.getImageLayout().getTileHeight(null));
        assertNull(imageHeader.getImageLayout().getColorModel(null));
        assertNotNull(imageHeader.getImageLayout().getSampleModel(null));
        assertEquals(sampleModel.getNumBands(), imageHeader.getImageLayout().getSampleModel(null).getNumBands());
        assertEquals(sampleModel.getDataType(), imageHeader.getImageLayout().getSampleModel(null).getDataType());
    }
}
