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

package com.bc.ceres.jai;

import junit.framework.TestCase;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

public class TileTest extends TestCase {
    
    public void testTilesHaveExpectedBounds() {
        int imageSize = 100;
        int tileSize = 50;
        Opi image;

        /////////////////////////////////////////////////////////////////////
        // minX = 0, tileGridOffset = 0
        //
        image = new Opi(0, 0, imageSize, imageSize, 0, 0, tileSize, tileSize);
        assertEquals(0, image.getTileGridXOffset());
        assertEquals(2, image.getNumXTiles());
        assertEquals(0, image.getMinTileX());
        assertEquals(1, image.getMaxTileX());
        assertEquals(0, image.tileXToX(0));
        assertEquals(50, image.tileXToX(1));

        assertOutOfBounds(image, new Rectangle(-1, 0, 1, 1));
        assertWithinBounds(image, new Rectangle(0, 0, 1, 1), "(0,0,50,50),(0,0,50,50);");
        assertWithinBounds(image, new Rectangle(50, 0, 1, 1), "(50,0,50,50),(50,0,50,50);");
        assertOutOfBounds(image, new Rectangle(100, 0, 1, 1));

        /////////////////////////////////////////////////////////////////////
        // minX = -20, tileGridOffset = 0
        //
        image = new Opi(-20, 0, imageSize, imageSize, 0, 0, tileSize, tileSize);
        assertEquals(0, image.getTileGridXOffset());
        assertEquals(3, image.getNumXTiles());
        assertEquals(-1, image.getMinTileX());
        assertEquals(1, image.getMaxTileX());
        assertEquals(-50, image.tileXToX(-1));
        assertEquals(0, image.tileXToX(0));
        assertEquals(50, image.tileXToX(1));

        assertOutOfBounds(image, new Rectangle(-21, 0, 1, 1));
        assertWithinBounds(image, new Rectangle(-20, 0, 1, 1), "(-50,0,50,50),(-20,0,20,50);");
        assertWithinBounds(image, new Rectangle(0, 0, 1, 1), "(0,0,50,50),(0,0,50,50);");
        assertWithinBounds(image, new Rectangle(50, 0, 1, 1), "(50,0,50,50),(50,0,30,50);");
        assertOutOfBounds(image, new Rectangle(-20 + 100, 0, 1, 1));

        /////////////////////////////////////////////////////////////////////
        // minX = 0, tileGridOffset = -15
        //
        image = new Opi(0, 0, imageSize, imageSize,
                        -15, 0, tileSize, tileSize);
        assertEquals(-15, image.getTileGridXOffset());
        assertEquals(3, image.getNumXTiles());
        assertEquals(0, image.getMinTileX());
        assertEquals(2, image.getMaxTileX());
        assertEquals(-15, image.tileXToX(0));
        assertEquals(35, image.tileXToX(1));
        assertEquals(85, image.tileXToX(2));

        assertOutOfBounds(image, new Rectangle(-15, 0, 1, 1));
        assertWithinBounds(image, new Rectangle(0, 0, 1, 1), "(-15,0,50,50),(0,0,35,50);");
        assertWithinBounds(image, new Rectangle(50, 0, 1, 1), "(35,0,50,50),(35,0,50,50);");
        assertWithinBounds(image, new Rectangle(99, 0, 1, 1), "(85,0,50,50),(85,0,15,50);");
        assertOutOfBounds(image, new Rectangle(100, 0, 1, 1));

        /////////////////////////////////////////////////////////////////////
        // minX = -20, tileGridOffset = -15
        //
        image = new Opi(-20, 0, imageSize, imageSize,
                        -15, 0, tileSize, tileSize);
        assertEquals(-15, image.getTileGridXOffset());
        assertEquals(3, image.getNumXTiles());
        assertEquals(-1, image.getMinTileX());
        assertEquals(1, image.getMaxTileX());
        assertEquals(-15, image.tileXToX(0));
        assertEquals(35, image.tileXToX(1));
        assertEquals(85, image.tileXToX(2));

        assertOutOfBounds(image, new Rectangle(-21, 0, 1, 1));
        assertWithinBounds(image, new Rectangle(-20, 0, 1, 1), "(-65,0,50,50),(-20,0,5,50);");
        assertWithinBounds(image, new Rectangle(0, 0, 1, 1), "(-15,0,50,50),(-15,0,50,50);");
        assertWithinBounds(image, new Rectangle(50, 0, 1, 1), "(35,0,50,50),(35,0,45,50);");
        assertOutOfBounds(image, new Rectangle(-20 + 100, 0, 1, 1));
    }

    private static void assertOutOfBounds(Opi image, Rectangle region) {
        try {
            image.getData(region);
            fail("The specified region "+rstring(region)+" should not intersect with the image`s bounds " + rstring(image.getBounds()));
        } catch (IllegalArgumentException e) {
            assertEquals("The specified region, if not null, must intersect with the image`s bounds.", e.getMessage());
        }
    }

    private static void assertWithinBounds(Opi image, Rectangle input, String expected) {
        image.trace = "";
        image.getData(input);
        assertEquals(expected, image.trace);
    }

    private static String rstring(Rectangle rectangle) {
        return "(" + rectangle.x + "," + rectangle.y + "," + rectangle.width + "," + rectangle.height + ")";
    }

    static class Opi extends SourcelessOpImage {
        String trace = "";

        Opi(int minX, int minY, int width, int height, int tileGridXOffset, int tileGridYOffset, int tileWidth, int tileHeight) {
            super(new ImageLayout(minX, minY, width, height, tileGridXOffset, tileGridYOffset, tileWidth, tileHeight, null, null), null, new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, new int[]{0}), minX, minY, width, height);
        }

        @Override
        protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
            trace += rstring(dest.getBounds()) + "," + rstring(destRect) + ";";
        }

    }
}
