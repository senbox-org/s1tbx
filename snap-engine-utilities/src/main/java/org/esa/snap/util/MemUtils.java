/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

//import org.jaitools.tilecache.DiskMemTileCache;

import org.esa.beam.util.jai.JAIUtils;

import javax.media.jai.JAI;
import java.awt.*;

/**
 * memory utils
 */
public class MemUtils {

    /**
     * Empty all tiles from cache and garbage collect
     */
    public static void freeAllMemory() {
        JAI.getDefaultInstance().getTileCache().flush();
        System.gc();
        System.gc();
        System.gc();
    }

    /**
     * tell tileCache that some old tiles can be removed
     */
    public static void tileCacheFreeOldTiles() {
        JAI.getDefaultInstance().getTileCache().memoryControl();
    }

    public static void configureJaiTileCache() {
        final int tileCacheCapacity = Integer.parseInt(System.getProperty("jai.tileCache.memoryCapacity", "512"));
        JAIUtils.setDefaultTileCacheCapacity(tileCacheCapacity);
        final int tileSize = Integer.parseInt(System.getProperty("jai.tileSize", "256"));
        JAI.setDefaultTileSize(new Dimension(tileSize, tileSize));
        JAI.getDefaultInstance().setRenderingHint(
                JAI.KEY_CACHED_TILE_RECYCLING_ENABLED,
                Boolean.TRUE);
    }

    public static void createTileCache() {

        //final Map<String, Object> cacheParams = new HashMap<String, Object>();
        //cacheParams.put(DiskMemTileCache.KEY_INITIAL_MEMORY_CAPACITY, 1L * 1024 * 1024);

        //final DiskMemTileCache cache = new DiskMemTileCache();
        //cache.setAutoFlushMemoryEnabled(true);

        //final SwappingTileCache cache = new SwappingTileCache();

        // JAI.getDefaultInstance().setTileCache( cache );
    }
}
