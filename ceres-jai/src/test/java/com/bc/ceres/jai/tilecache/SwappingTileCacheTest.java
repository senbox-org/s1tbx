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

package com.bc.ceres.jai.tilecache;

import junit.framework.TestCase;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.HashMap;

public class SwappingTileCacheTest extends TestCase {


    public void testTileStoreRestore() {
        long tileSize = 256 * 256 * 4;

        TiledImage im0 = createImage(4, 4);
        SwapSpaceMock swapSpaceMock = new SwapSpaceMock();
        SwappingTileCache cache = new SwappingTileCache(3 * tileSize + 1, swapSpaceMock);

        Raster tile00 = im0.getTile(0, 0);
        Raster tile10 = im0.getTile(1, 0);
        Raster tile01 = im0.getTile(0, 1);
        Raster tile32 = im0.getTile(3, 2);

        swapSpaceMock.trace = "";
        cache.add(im0, 0, 0, tile00);
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im0, 1, 0, tile10);
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im0, 0, 1, tile01);
        assertEquals("", swapSpaceMock.trace);

        // Expected: 2 swapped tiles LRU order. Two tiles, because memoryThreshold=75%
        swapSpaceMock.trace = "";
        cache.add(im0, 3, 2, tile32);
        assertEquals("" +
                "storeTile(0-0-0);" +
                "storeTile(0-1-0);",
                     swapSpaceMock.trace);

        assertTrue(swapSpaceMock.containsTile(im0, 0, 0));
        assertTrue(swapSpaceMock.containsTile(im0, 1, 0));
        assertFalse(swapSpaceMock.containsTile(im0, 0, 1));
        assertFalse(swapSpaceMock.containsTile(im0, 3, 2));

        swapSpaceMock.trace = "";
        Raster tile00r = cache.getTile(im0, 0, 0);
        assertEquals("" +
                "restoreTile(0-0-0)=MemoryTile;",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        Raster tile10r = cache.getTile(im0, 1, 0);
        assertEquals("" +
                "restoreTile(0-1-0)=MemoryTile;" +
                "storeTile(0-0-1);" +
                "storeTile(0-3-2);",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        Raster tile01r = cache.getTile(im0, 0, 1);
        assertEquals("" +
                "restoreTile(0-0-1)=MemoryTile;",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        Raster tile32r = cache.getTile(im0, 3, 2);
        assertEquals("" +
                "restoreTile(0-3-2)=MemoryTile;" +
                "storeTile(0-0-0);" +
                "storeTile(0-1-0);",
                     swapSpaceMock.trace);

        // Expected: failed restore, bacause tile does not exist
        swapSpaceMock.trace = "";
        Raster tile33r = cache.getTile(im0, 3, 3);
        assertEquals("restoreTile(0-3-3)=null;", swapSpaceMock.trace);

        testEqualTile(tile00, tile00r);
        testEqualTile(tile10, tile10r);
        testEqualTile(tile01, tile01r);
        testEqualTile(tile32, tile32r);
        assertNull(tile33r);

        swapSpaceMock.trace = "";
        cache.remove(im0, 0, 0);
        cache.remove(im0, 0, 1);
        cache.remove(im0, 2, 2);
        assertEquals("" +
                "deleteTile(0-0-0)=true;" +
                "deleteTile(0-0-1)=true;" +
                "deleteTile(0-2-2)=false;",
                     swapSpaceMock.trace);

        assertFalse(swapSpaceMock.containsTile(im0, 0, 0));
        assertTrue(swapSpaceMock.containsTile(im0, 1, 0));
        assertFalse(swapSpaceMock.containsTile(im0, 0, 1));
        assertTrue(swapSpaceMock.containsTile(im0, 3, 2));

        TiledImage im1 = createImage(4, 4);

        swapSpaceMock.trace = "";
        cache.add(im1, 0, 0, im1.getTile(0, 0));
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 0, 1, im1.getTile(0, 1));
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 0, 2, im1.getTile(0, 2));
        assertEquals("" +
                "storeTile(0-3-2);" +
                "storeTile(1-0-0);",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 0, 3, im1.getTile(0, 3));
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 1, 0, im1.getTile(1, 0));
        assertEquals("" +
                "storeTile(1-0-1);" +
                "storeTile(1-0-2);",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 1, 1, im1.getTile(1, 1));
        assertEquals("", swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.add(im1, 1, 2, im1.getTile(1, 2));
        assertEquals("" +
                "storeTile(1-0-3);" +
                "storeTile(1-1-0);",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.removeTiles(im0);
        assertEquals("" +
                "deleteTile(0-0-0)=false;" +
                "deleteTile(0-1-0)=true;" +
                "deleteTile(0-2-0)=false;" +
                "deleteTile(0-3-0)=false;" +
                "deleteTile(0-0-1)=false;" +
                "deleteTile(0-1-1)=false;" +
                "deleteTile(0-2-1)=false;" +
                "deleteTile(0-3-1)=false;" +
                "deleteTile(0-0-2)=false;" +
                "deleteTile(0-1-2)=false;" +
                "deleteTile(0-2-2)=false;" +
                "deleteTile(0-3-2)=true;" +
                "deleteTile(0-0-3)=false;" +
                "deleteTile(0-1-3)=false;" +
                "deleteTile(0-2-3)=false;" +
                "deleteTile(0-3-3)=false;",
                     swapSpaceMock.trace);

        swapSpaceMock.trace = "";
        cache.removeTiles(im1);
        assertEquals("" +
                "deleteTile(1-0-0)=true;" +
                "deleteTile(1-1-0)=true;" +
                "deleteTile(1-2-0)=false;" +
                "deleteTile(1-3-0)=false;" +
                "deleteTile(1-0-1)=true;" +
                "deleteTile(1-1-1)=false;" +
                "deleteTile(1-2-1)=false;" +
                "deleteTile(1-3-1)=false;" +
                "deleteTile(1-0-2)=true;" +
                "deleteTile(1-1-2)=false;" +
                "deleteTile(1-2-2)=false;" +
                "deleteTile(1-3-2)=false;" +
                "deleteTile(1-0-3)=true;" +
                "deleteTile(1-1-3)=false;" +
                "deleteTile(1-2-3)=false;" +
                "deleteTile(1-3-3)=false;",
                     swapSpaceMock.trace);
    }

    private static TiledImage createImage(int numXTiles, int numYTiles) {
        ComponentSampleModelJAI sm = new ComponentSampleModelJAI(DataBuffer.TYPE_FLOAT, 256, 256, 1, 256, new int[1]);
        return new TiledImage(0, 0, numXTiles * 256, numYTiles * 256, 0, 0, sm, PlanarImage.createColorModel(sm));
    }

    private void testEqualTile(Raster tile00, Raster tile00r) {
        assertNotNull(tile00);
        assertNotNull(tile00r);
        assertEquals(tile00.getWidth(), tile00r.getWidth());
        assertEquals(tile00.getHeight(), tile00r.getHeight());
        assertEquals(tile00.getSampleModel(), tile00r.getSampleModel());
    }


    private static class SwapSpaceMock implements SwapSpace {
        HashMap<RenderedImage, Integer> ids = new HashMap<RenderedImage, Integer>();
        HashMap<String, MemoryTile> tiles = new HashMap<String, MemoryTile>();
        String trace = "";

        public boolean containsTile(RenderedImage owner, int tileX, int tileY) {
            String key = getKey(owner, tileX, tileY);
            return tiles.containsKey(key);
        }

        public boolean storeTile(MemoryTile memoryTile) {
            String key = getKey(memoryTile.getOwner(), memoryTile.getTileX(), memoryTile.getTileY());
            tiles.put(key, memoryTile);
            trace += "storeTile(" + key + ");";
            return true;
        }

        public MemoryTile restoreTile(RenderedImage owner, int tileX, int tileY) {
            String key = getKey(owner, tileX, tileY);
            final MemoryTile memoryTile = tiles.get(key);
            trace += "restoreTile(" + key + ")=" + (memoryTile != null ? "MemoryTile" : "null") + ";";
            return memoryTile;
        }

        public boolean deleteTile(RenderedImage owner, int tileX, int tileY) {
            String key = getKey(owner, tileX, tileY);
            final boolean b = tiles.remove(key) != null;
            trace += "deleteTile(" + key + ")=" + b + ";";
            return b;
        }

        private int getId(RenderedImage owner) {
            Integer integer = ids.get(owner);
            if (integer != null) {
                return integer;
            }
            integer = ids.size();
            ids.put(owner, integer);
            return integer;
        }

        private String getKey(RenderedImage owner, int tileX, int tileY) {
            return getId(owner) + "-" + tileX + "-" + tileY;
        }
    }
}
