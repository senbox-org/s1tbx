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

package org.esa.snap.core.gpf.internal;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.HashMap;

public abstract class AbstractTileImageTileTest extends TestCase {

    private Product product;
    private HashMap<String, TestOpImage> imageMap;

    @Override
    protected void setUp() throws Exception {
        Dimension imageSize = getImageSize();
        Dimension tileSize = getTileSize();
        product = new Product("N", "T", imageSize.width, imageSize.height);
        product.setPreferredTileSize(tileSize.width, tileSize.height);
        product.addBand("B_INT8", ProductData.TYPE_INT8);
        product.addBand("B_UINT8", ProductData.TYPE_UINT8);
        product.addBand("B_INT16", ProductData.TYPE_INT8);
        product.addBand("B_UINT16", ProductData.TYPE_UINT8);
        product.addBand("B_INT32", ProductData.TYPE_INT8);
        product.addBand("B_UINT32", ProductData.TYPE_UINT8);
        product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);
        product.addBand("B_FLOAT64", ProductData.TYPE_FLOAT64);
        imageMap = new HashMap<String, TestOpImage>();
    }

    @Override
    protected void tearDown() throws Exception {
        for (TestOpImage testOpImage : imageMap.values()) {
            testOpImage.dispose();
        }
        imageMap.clear();
        product.dispose();
        product = null;
    }

    public abstract Dimension getImageSize();

    public abstract Dimension getTileSize();

    public abstract int getTileCount();

    public Product getProduct() {
        return product;
    }

    public Band getBand(String name) {
        return product.getBand(name);
    }

    public TestOpImage getImage(String name) {
        Band band = getBand(name);
        assertNotNull(band);
        TestOpImage image = imageMap.get(name);
        if (image == null) {
            image = new TestOpImage(band);
            final Raster[] rasters = image.getTiles(); // Forces JAI tile computation.
            assertNotNull(rasters);
            assertEquals(getTileCount(), rasters.length);
            imageMap.put(name, image);

            Dimension tileSize = getTileSize();
            assertEquals(tileSize.width, image.getSampleModel().getWidth());
            assertEquals(tileSize.height, image.getSampleModel().getHeight());
        }
        return image;
    }

    protected void testTileStructure(TileImpl tile,
                                     Rectangle expectedRect,
                                     int expectedScanlineOffset,
                                     int expectedScanlineStride,
                                     boolean expectedTarget) {
        assertEquals(expectedRect, tile.getRectangle());
        assertEquals(expectedRect.x, tile.getMinX());
        assertEquals(expectedRect.y, tile.getMinY());
        assertEquals(expectedRect.x + expectedRect.width - 1, tile.getMaxX());
        assertEquals(expectedRect.y + expectedRect.height - 1, tile.getMaxY());
        assertEquals(expectedRect.width, tile.getWidth());
        assertEquals(expectedRect.height, tile.getHeight());
        assertEquals(expectedScanlineStride, tile.getScanlineStride());
        assertEquals(expectedScanlineOffset, tile.getScanlineOffset());
        assertEquals(expectedTarget, tile.isTarget());
    }

    protected Raster getImageData(TestOpImage image, Rectangle expectedRect) {
        Raster raster = image.getData(expectedRect);
        assertEquals(expectedRect.x, raster.getMinX());
        assertEquals(expectedRect.y, raster.getMinY());
        assertEquals(expectedRect.width, raster.getWidth());
        assertEquals(expectedRect.height, raster.getHeight());
        return raster;
    }

    protected void testOnlySamplesFloatAccessible(TileImpl tile) {
        assertNull(tile.getDataBufferByte());
        assertNull(tile.getDataBufferShort());
        assertNull(tile.getDataBufferInt());
        assertNotNull(tile.getDataBufferFloat());
        assertNull(tile.getDataBufferDouble());
    }

    protected void testFloatSample32IO(TileImpl tile, int x0, int y0) {
        tile.setSample(x0, y0, false); // boolean
        assertEquals(false, tile.getSampleBoolean(x0, y0));
        assertEquals(0, tile.getSampleInt(x0, y0));
        assertEquals(0.0f, tile.getSampleFloat(x0, y0), 1e-5f);
        assertEquals(0.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, true); // boolean
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(1, tile.getSampleInt(x0, y0));
        assertEquals(1.0f, tile.getSampleFloat(x0, y0), 1e-5f);
        assertEquals(1.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, 1234); // int
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(1234, tile.getSampleInt(x0, y0));
        assertEquals(1234.0f, tile.getSampleFloat(x0, y0), 1e-5f);
        assertEquals(1234.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, 67.89f);  // float
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(67, tile.getSampleInt(x0, y0));
        assertEquals(67.89f, tile.getSampleFloat(x0, y0), 1e-5f);
        assertEquals(67.89, tile.getSampleDouble(x0, y0), 1e-5);

        tile.setSample(x0, y0, 12.34567890123);  // double
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(12, tile.getSampleInt(x0, y0));
        assertEquals(12.345679f, tile.getSampleFloat(x0, y0), 1e-5f);
        assertEquals(12.345679, tile.getSampleDouble(x0, y0), 1e-5);
    }

    protected void testFloat64IO(TileImpl tile, int x0, int y0) {
        tile.setSample(x0, y0, false); // boolean
        assertEquals(false, tile.getSampleBoolean(x0, y0));
        assertEquals(0, tile.getSampleInt(x0, y0));
        assertEquals(0.0f, tile.getSampleFloat(x0, y0), 1e-10f);
        assertEquals(0.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, true); // boolean
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(1, tile.getSampleInt(x0, y0));
        assertEquals(1.0f, tile.getSampleFloat(x0, y0), 1e-10f);
        assertEquals(1.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, 1234); // int
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(1234, tile.getSampleInt(x0, y0));
        assertEquals(1234.0f, tile.getSampleFloat(x0, y0), 1e-10f);
        assertEquals(1234.0, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, 67.89f);  // float
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(68, tile.getSampleInt(x0, y0));
        assertEquals(67.89f, tile.getSampleFloat(x0, y0), 1e-10f);
        assertEquals(67.89, tile.getSampleDouble(x0, y0), 1e-10);

        tile.setSample(x0, y0, 12.34567890123);  // double
        assertEquals(true, tile.getSampleBoolean(x0, y0));
        assertEquals(12, tile.getSampleInt(x0, y0));
        assertEquals(12.3456f, tile.getSampleFloat(x0, y0), 1e-10f);
        assertEquals(12.34567890123, tile.getSampleDouble(x0, y0), 1e-10);
    }

    protected void testFloat32RawSampleIO(TileImpl tile, int x, int y) {
        ProductData samplesGeneric = tile.getDataBuffer();
        assertNotNull(samplesGeneric);

        float[] samplesFloat = tile.getDataBufferFloat();
        assertNotNull(samplesFloat);

        assertSame(samplesFloat, samplesGeneric.getElems());

        int lineOffset = tile.getScanlineOffset();
        int lineStride = tile.getScanlineStride();
        int index = lineOffset + (y - tile.getMinY()) * lineStride + (x - tile.getMinX());

        assertTrue(index >= 0);
        assertTrue(index < samplesFloat.length);
        assertTrue(index < samplesGeneric.getNumElems());
        assertEquals(index, tile.getDataBufferIndex(x, y));

        samplesFloat[index] = 1234.56f;
        assertEquals(1234.56f, samplesFloat[index], 1e-5f);
        assertEquals(1234.56f, samplesGeneric.getElemFloatAt(index), 1e-5f);
        assertEquals(1234.56f, tile.getSampleFloat(x, y), 1e-5f);

        samplesGeneric.setElemFloatAt(index, 213.536f);
        assertEquals(213.536f, samplesFloat[index], 1e-5f);
        assertEquals(213.536f, samplesGeneric.getElemFloatAt(index), 1e-5f);
        assertEquals(213.536f, tile.getSampleFloat(x, y), 1e-5f);

        samplesGeneric.setElemIntAt(index, 707);
        assertEquals(707.0f, samplesFloat[index], 1e-5f);
        assertEquals(707.0f, samplesGeneric.getElemFloatAt(index), 1e-5f);
        assertEquals(707.0f, tile.getSampleFloat(x, y), 1e-5f);
    }

}
