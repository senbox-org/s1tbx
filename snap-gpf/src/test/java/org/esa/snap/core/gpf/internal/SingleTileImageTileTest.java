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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;

public class SingleTileImageTileTest extends AbstractTileImageTileTest {
    final static int IMAGE_W = 4;
    final static int IMAGE_H = 5;
    private TestOpImage imageFLOAT32;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageFLOAT32 = getImage("B_FLOAT32");
    }

    @Override
    public int getTileCount() {
        return 1;
    }

    @Override
    public Dimension getImageSize() {
        return new Dimension(IMAGE_W, IMAGE_H);
    }

    @Override
    public Dimension getTileSize() {
        return new Dimension(IMAGE_W, IMAGE_H);
    }

    public void testThatImageIsNotTiled() {
        assertEquals(1, imageFLOAT32.getTileCount());
        assertEquals(IMAGE_W, imageFLOAT32.getSampleModel().getWidth());
        assertEquals(IMAGE_H, imageFLOAT32.getSampleModel().getHeight());
    }

    public void testFullTile() {

        Rectangle expectedRect = new Rectangle(IMAGE_W, IMAGE_H);
        TileImpl tile = imageFLOAT32.getTile(expectedRect);
        assertNotNull(tile);
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());

        testTileStructure(tile, expectedRect, 0, IMAGE_W, true);
        testOnlySamplesFloatAccessible(tile);

        // test for initial sample values
        assertEquals(0.5, tile.getSampleDouble(0, 0), 1e-5);
        assertEquals(1.5, tile.getSampleDouble(0, 1), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(0, 2), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(0, 3), 1e-5);
        assertEquals(10.5, tile.getSampleDouble(1, 0), 1e-5);
        assertEquals(20.5, tile.getSampleDouble(2, 0), 1e-5);
        assertEquals(30.5, tile.getSampleDouble(3, 0), 1e-5);
        assertEquals(11.5, tile.getSampleDouble(1, 1), 1e-5);
        assertEquals(22.5, tile.getSampleDouble(2, 2), 1e-5);
        assertEquals(33.5, tile.getSampleDouble(3, 3), 1e-5);

        testFloat32RawSampleIO(tile, 0, 0);
        testFloat32RawSampleIO(tile, 0, IMAGE_H - 1);
        testFloat32RawSampleIO(tile, IMAGE_W - 1, 0);
        testFloat32RawSampleIO(tile, IMAGE_W - 1, IMAGE_H - 1);
        testFloat32RawSampleIO(tile, IMAGE_W / 2, IMAGE_H / 2);
    }

    public void testChildTile() {
        final int CHILD_X = 1;
        final int CHILD_Y = 2;
        final int CHILD_W = 2;
        final int CHILD_H = 3;

        Rectangle expectedRect = new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H);
        Raster childRaster = getImageData(imageFLOAT32, expectedRect);
        TileImpl tile = new TileImpl(getBand("B_FLOAT32"), childRaster, expectedRect, false);
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());

        testTileStructure(tile, expectedRect, CHILD_Y * IMAGE_W + CHILD_X, IMAGE_W, false);
        testOnlySamplesFloatAccessible(tile);

        // test for initial sample values
        assertEquals(12.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 0), 1e-5);
        assertEquals(22.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 0), 1e-5);
        assertEquals(13.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 1), 1e-5);
        assertEquals(23.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 1), 1e-5);
        assertEquals(14.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 2), 1e-5);
        assertEquals(24.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 2), 1e-5);

        testFloat32RawSampleIO(tile, CHILD_X + 0, CHILD_Y + 0);
        testFloat32RawSampleIO(tile, CHILD_X + 1, CHILD_Y + 0);
        testFloat32RawSampleIO(tile, CHILD_X + 0, CHILD_Y + 1);
        testFloat32RawSampleIO(tile, CHILD_X + 1, CHILD_Y + 1);
        testFloat32RawSampleIO(tile, CHILD_X + 0, CHILD_Y + 2);
        testFloat32RawSampleIO(tile, CHILD_X + 1, CHILD_Y + 2);
    }
}
