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

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default implementation of the {@link SwapSpace} interface.
 * It creates a file for each swapped tile in the given swap directory.
 *
 * @author Norman Fomferra
 */
public class DefaultSwapSpace implements SwapSpace {
    private final File swapDir;
    private final Logger logger;
    private final Map<Object, SwappedTile> swappedTiles;

    public DefaultSwapSpace(File swapDir) {
        this(swapDir, Logger.getLogger(System.getProperty("ceres.context", "ceres")));
    }

    public DefaultSwapSpace(File swapDir, Logger logger) {
        this.swapDir = swapDir;
        this.logger = logger;
        this.swappedTiles = new HashMap<Object, SwappedTile>(1009); // prime number
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (Object key : swappedTiles.keySet()) {
            swappedTiles.get(key).delete();
        }
    }

    public synchronized boolean storeTile(MemoryTile mt) {
        SwappedTile st = swappedTiles.get(mt.getKey());
        if (st == null) {
            try {
                st = new SwappedTile(mt, swapDir);
                if (!st.isAvailable()) {
                    final long t1 = System.currentTimeMillis();
                    st.storeTile(mt.getTile());
                    final long t2 = System.currentTimeMillis();
                    st.getFile().deleteOnExit();
                    logger.log(Level.FINEST, "Tile stored: " + st.getFile() + " (" + (t2 - t1) + " ms)");
                }
                swappedTiles.put(mt.getKey(), st);
                return true;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Tile NOT stored: " + st.getFile(), e);
                handleTileStoreFailed(mt, e);
            }
        }
        return false;
    }

    public synchronized MemoryTile restoreTile(RenderedImage owner, int tileX, int tileY) {
        final Object key = hashKey(owner, tileX, tileY);
        final SwappedTile st = swappedTiles.get(key);
        if (st == null) {
            return null;
        }
        try {
            final long t1 = System.currentTimeMillis();
            final Raster tile = st.restoreTile();
            final long t2 = System.currentTimeMillis();
            logger.log(Level.FINEST, "Tile restored: " + st.getFile()  + " (" + (t2 - t1) + " ms)");
            return new MemoryTile(owner, tileX, tileY, tile, st.getTileCacheMetric());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Tile NOT restored: " + st.getFile());
            return handleTileRestoreFailed(owner, tileX, tileY, e);
        }
    }

    public synchronized boolean deleteTile(RenderedImage owner, int tileX, int tileY) {
        final Object key = hashKey(owner, tileX, tileY);
        final SwappedTile st = swappedTiles.remove(key);
        if (st == null || !st.getFile().exists()) {
            return false;
        }
        final boolean deleted = st.delete();
        if (deleted) {
            logger.log(Level.FINEST, "Tile deleted: " + st.getFile());
        } else {
            logger.log(Level.WARNING, "Tile NOT deleted: " + st.getFile());
        }
        return deleted;
    }

    protected void handleTileStoreFailed(MemoryTile mt, IOException e) {
        deleteTile(mt.getOwner(), mt.getTileX(), mt.getTileY());
    }

    protected MemoryTile handleTileRestoreFailed(RenderedImage owner, int tileX, int tileY, IOException e) {
        return null;
    }

    private static Object hashKey(RenderedImage owner, int tileX, int tileY) {
        return MemoryTile.hashKey(owner, tileX, tileY);
    }

}
