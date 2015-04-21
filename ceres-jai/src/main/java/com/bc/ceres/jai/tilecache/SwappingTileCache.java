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

import com.sun.media.jai.util.CacheDiagnostics;
import com.sun.media.jai.util.ImageUtil;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.TileCache;
import javax.media.jai.util.ImagingListener;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * A tile cache based on Sun Microsystems' reference implementation of the
 * <code>javax.media.jai.TileCache</code> interface. In opposite to the
 * Sun implementation, we'll never throw away any tiles but instead swap them to a
 * {@link SwapSpace}.
 *
 * @author Sun Microsystems
 * @author Norman Fomferra
 */
public final class SwappingTileCache extends Observable implements TileCache, CacheDiagnostics {

    /**
     * The default memory capacity of the cache (16 MB).
     */
    public static final long DEFAULT_MEMORY_CAPACITY = 16L * 1024L * 1024L;

    /**
     * The default hashtable capacity (heuristic)
     */
    public static final int DEFAULT_HASHTABLE_CAPACITY = 1009; // prime number

    /**
     * The default directory where tiles are stored if they don't fit into memory anymore.
     */
    public static final File DEFAULT_SWAP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /**
     * The hashtable load factor
     */
    private static final float LOAD_FACTOR = 0.5F;

    /**
     * The tile cache.
     * A Hashtable is used to cache the tiles.  The "key" is a
     * <code>Object</code> determined based on tile owner's UID if any or
     * hashCode if the UID doesn't exist, and tile index.  The
     * "value" is a SunCachedTile.
     */
    private Hashtable<Object, MemoryTile> cache;

    /**
     * Sorted (Tree) Set used with tile metrics.
     * Adds another level of metrics used to determine
     * which tiles are removed during memoryControl().
     */
    private SortedSet<MemoryTile> cacheSortedSet;

    /**
     * The memory capacity of the cache.
     */
    private long memoryCapacity;

    /**
     * The amount of memory currently being used by the cache.
     */
    private long memoryUsage = 0;

    /**
     * The amount of memory to keep after memory control
     */
    private float memoryThreshold = 0.75F;

    /**
     * A indicator for tile access time.
     */
    private long timeStamp = 0;

    /**
     * Custom comparator used to determine tile cost or
     * priority ordering in the tile cache.
     */
    private Comparator comparator = null;

    /**
     * Pointer to the first (newest) tile of the linked SunCachedTile list.
     */
    private MemoryTile first = null;

    /**
     * Pointer to the last (oldest) tile of the linked SunCachedTile list.
     */
    private MemoryTile last = null;

    /**
     * Tile count used for diagnostics
     */
    private long tileCount = 0;

    /**
     * Cache hit count
     */
    private long hitCount = 0;

    /**
     * Cache miss count
     */
    private long missCount = 0;

    /**
     * Diagnostics enable/disable
     */
    private boolean diagnostics = false;

    private SwapSpace swapSpace;

    // diagnostic actions
    // !!! If actions are changed in any way (removal, modification, addition)
    // then the getCachedTileActions() method below should be changed to match.
    private static final int ADD = 0;
    private static final int REMOVE = 1;
    private static final int REMOVE_FROM_FLUSH = 2;
    private static final int REMOVE_FROM_MEMCON = 3;
    private static final int UPDATE_FROM_ADD = 4;
    private static final int UPDATE_FROM_GETTILE = 5;
    private static final int ABOUT_TO_REMOVE = 6;

    /**
     * @return An array of <code>EnumeratedParameter</code>s corresponding
     *         to the numeric values returned by the <code>getAction()</code>
     *         method of the <code>CachedTile</code> implementation used by
     *         <code>SunTileCache</code>.  The "name" of each
     *         <code>EnumeratedParameter</code> provides a brief string
     *         describing the numeric action value.
     */
    public static EnumeratedParameter[] getCachedTileActions() {
        return new EnumeratedParameter[]{
                new EnumeratedParameter("add", ADD),
                new EnumeratedParameter("remove", REMOVE),
                new EnumeratedParameter("remove_by_flush", REMOVE_FROM_FLUSH),
                new EnumeratedParameter("remove_by_memorycontrol",
                                        REMOVE_FROM_MEMCON),
                new EnumeratedParameter("timestamp_update_by_add", UPDATE_FROM_ADD),
                new EnumeratedParameter("timestamp_update_by_gettile",
                                        UPDATE_FROM_GETTILE),
                new EnumeratedParameter("preremove", ABOUT_TO_REMOVE)
        };
    }

    /**
     * No args constructor. Use the DEFAULT_MEMORY_CAPACITY of 16 Megs.
     */
    public SwappingTileCache() {
        this(DEFAULT_MEMORY_CAPACITY, new DefaultSwapSpace(DEFAULT_SWAP_DIR));
    }

    /**
     * Constructor.  The memory capacity should be explicitly specified.
     *
     * @param memoryCapacity The maximum cache memory size in bytes.
     * @param swapSpace      The space used to swap out tiles.
     * @throws IllegalArgumentException If <code>memoryCapacity</code>
     *                                  is less than 0.
     */
    public SwappingTileCache(long memoryCapacity, SwapSpace swapSpace) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException("memoryCapacity < 0");
        }
        if (swapSpace == null) {
            throw new NullPointerException("swapSpace");
        }

        this.memoryCapacity = memoryCapacity;
        this.swapSpace = swapSpace;

        // try to get a prime number (more efficient?)
        // lower values of LOAD_FACTOR increase speed, decrease space efficiency
        cache = new Hashtable<Object, MemoryTile>(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);
    }


    /**
     * Adds a tile to the cache.
     * <p> If the specified tile is already in the cache, it will not be
     * cached again.  If by adding this tile, the cache exceeds the memory
     * capacity, older tiles in the cache are removed to keep the cache
     * memory usage under the specified limit.
     *
     * @param owner The image the tile blongs to.
     * @param tileX The tile's X index within the image.
     * @param tileY The tile's Y index within the image.
     * @param tile  The tile to be cached.
     */
    public void add(RenderedImage owner,
                    int tileX,
                    int tileY,
                    Raster tile) {
        add(owner, tileX, tileY, tile, null);
    }

    /**
     * Adds a tile to the cache with an associated tile compute cost.
     * <p> If the specified tile is already in the cache, it will not be
     * cached again.  If by adding this tile, the cache exceeds the memory
     * capacity, older tiles in the cache are removed to keep the cache
     * memory usage under the specified limit.
     *
     * @param owner           The image the tile blongs to.
     * @param tileX           The tile's X index within the image.
     * @param tileY           The tile's Y index within the image.
     * @param tile            The tile to be cached.
     * @param tileCacheMetric Metric for prioritizing tiles
     */
    public synchronized void add(RenderedImage owner,
                                 int tileX,
                                 int tileY,
                                 Raster tile,
                                 Object tileCacheMetric) {

        if (memoryCapacity == 0) {
            return;
        }
        addTileNonSync(owner, tileX, tileY, tile, tileCacheMetric);
    }

    /**
     * Adds an array of tiles to the tile cache.
     *
     * @param owner           The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices     An array of <code>Point</code>s containing the
     *                        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @param tiles           The array of tile <code>Raster</code>s containing tile data.
     * @param tileCacheMetric Object which provides an ordering metric
     *                        associated with the <code>RenderedImage</code> owner.
     * @since 1.1
     */
    public synchronized void addTiles(RenderedImage owner,
                                      Point[] tileIndices,
                                      Raster[] tiles,
                                      Object tileCacheMetric) {
        if (memoryCapacity == 0) {
            return;
        }
        for (int i = 0; i < tileIndices.length; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;
            Raster tile = tiles[i];
            addTileNonSync(owner, tileX, tileY, tile, tileCacheMetric);
        }
    }

    private void addTileNonSync(RenderedImage owner, int tileX, int tileY, Raster tile, Object tileCacheMetric) {
        Object key = MemoryTile.hashKey(owner, tileX, tileY);
        MemoryTile ct = cache.get(key);

        if (ct != null) {
            ct.timeStamp = timeStamp++;

            if (ct != first) {
                // Bring this tile to the beginning of the list.
                if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = first;

                first.previous = ct;
                first = ct;
            }

            hitCount++;

            if (diagnostics) {
                ct.action = UPDATE_FROM_ADD;
                setChanged();
                notifyObservers(ct);
            }
        } else {
            ct = new MemoryTile(owner, tileX, tileY, tile, tileCacheMetric);
            addTileNonSync(ct);
        }
    }

    private boolean addTileNonSync(MemoryTile ct) {
        // Don't cache tile if adding it would provoke memoryControl()
        // which would in turn only end up removing the tile.
        if (memoryUsage + ct.tileSize > memoryCapacity &&
                ct.tileSize > (long) (memoryCapacity * memoryThreshold)) {
            return false;
        }

        ct.timeStamp = timeStamp++;
        ct.previous = null;
        ct.next = first;

        if (first == null && last == null) {
            first = ct;
            last = ct;
        } else {
            if (first == null) {
                throw new IllegalStateException("first == null");
            }
            first.previous = ct;
            first = ct;        // put this tile at the top of the list
        }

        // add to tile cache
        if (cache.put(ct.key, ct) == null) {
            memoryUsage += ct.tileSize;
            tileCount++;
            //missCount++;  Not necessary?

            if (cacheSortedSet != null) {
                cacheSortedSet.add(ct);
            }

            if (diagnostics) {
                ct.action = ADD;
                setChanged();
                notifyObservers(ct);
            }
        }
        // Bring memory usage down to memoryThreshold % of memory capacity.
        if (memoryUsage > memoryCapacity) {
            memoryControl();
        }
        return true;
    }

    /**
     * Removes a tile from the cache.
     * <p> If the specified tile is not in the cache, this method
     * does nothing.
     */
    public synchronized void remove(RenderedImage owner,
                                    int tileX,
                                    int tileY) {
        if (memoryCapacity == 0) {
            return;
        }
        removeNonSync(owner, tileX, tileY);
    }

    /**
     * Removes all the tiles that belong to a <code>RenderedImage</code>
     * from the cache.
     *
     * @param owner The image whose tiles are to be removed from the cache.
     */
    public synchronized void removeTiles(RenderedImage owner) {
        if (memoryCapacity == 0) {
            return;
        }
        int minTx = owner.getMinTileX();
        int minTy = owner.getMinTileY();
        int maxTx = minTx + owner.getNumXTiles();
        int maxTy = minTy + owner.getNumYTiles();
        for (int y = minTy; y < maxTy; y++) {
            for (int x = minTx; x < maxTx; x++) {
                removeNonSync(owner, x, y);
            }
        }
    }

    private void removeNonSync(RenderedImage owner, int tileX, int tileY) {
        Object key = MemoryTile.hashKey(owner, tileX, tileY);
        MemoryTile ct = cache.get(key);

        if (ct != null) {
            // Notify observers that a tile is about to be removed.
            // It is possible that the tile will be removed from the
            // cache before the observers get notified.  This should
            // be ok, since a hard reference to the tile will be
            // kept for the observers, so the garbage collector won't
            // remove the tile until the observers release it.
            ct.action = ABOUT_TO_REMOVE;
            setChanged();
            notifyObservers(ct);

            ct = cache.remove(key);

            // recalculate memoryUsage only if tile is actually removed
            if (ct != null) {
                memoryUsage -= ct.tileSize;
                tileCount--;

                if (cacheSortedSet != null) {
                    cacheSortedSet.remove(ct);
                }

                if (ct == first) {
                    if (ct == last) {
                        first = null;  // only one tile in the list
                        last = null;
                    } else {
                        first = ct.next;
                        first.previous = null;
                    }
                } else if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                // Notify observers that a tile has been removed.
                // If the core's hard references go away, the
                // soft references will be garbage collected.
                // Usually, by the time the observers are notified
                // the ct owner and tile are nulled by the GC, so
                // we can't really tell which op was removed
                // This occurs when OpImage's finalize method is
                // invoked.  This code works ok when remove is
                // called directly. (by flush() for example).
                // If the soft references are GC'd, the timeStamp
                // will no longer be contiguous, it will be
                // unique, so this is ok.
                if (diagnostics) {
                    ct.action = REMOVE;
                    setChanged();
                    notifyObservers(ct);
                }

                ct.previous = null;
                ct.next = null;
            }
        }
        // <NEW>
        swapSpace.deleteTile(owner, tileX, tileY);
        // </NEW>
    }

    /**
     * Retrieves a tile from the cache.
     * <p> If the specified tile is not in the cache, this method
     * returns <code>null</code>.  If the specified tile is in the
     * cache, its last-access time is updated.
     *
     * @param owner The image the tile blongs to.
     * @param tileX The tile's X index within the image.
     * @param tileY The tile's Y index within the image.
     */
    public synchronized Raster getTile(RenderedImage owner,
                                       int tileX,
                                       int tileY) {
        if (memoryCapacity == 0) {
            return null;
        }
        return getTileNonSync(owner, tileX, tileY);
    }

    /**
     * Retrieves a contiguous array of all tiles in the cache which are
     * owned by the specified image.  May be <code>null</code> if there
     * were no tiles in the cache.  The array contains no null entries.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cache.
     */
    public synchronized Raster[] getTiles(RenderedImage owner) {

        if (memoryCapacity == 0) {
            return null;
        }
        int size = Math.min(owner.getNumXTiles() * owner.getNumYTiles(),
                            (int) tileCount);

        if (size > 0) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();
            ArrayList<Raster> temp = new ArrayList<Raster>(32);
            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {
                    Raster tile = getTileNonSync(owner, x, y);
                    if (tile != null) {
                        temp.add(tile);
                    }
                }
            }
            if (!temp.isEmpty()) {
                return temp.toArray(new Raster[temp.size()]);
            }
        }

        return null;
    }

    /**
     * Returns an array of tile <code>Raster</code>s from the cache.
     * Any or all of the elements of the returned array may be <code>null</code>
     * if the corresponding tile is not in the cache.
     *
     * @param owner       The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices An array of <code>Point</code>s containing the
     *                    <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @since 1.1
     */
    public synchronized Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {
        if (memoryCapacity == 0) {
            return null;
        }
        Raster[] tiles = new Raster[tileIndices.length];
        for (int i = 0; i < tiles.length; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;
            tiles[i] = getTileNonSync(owner, tileX, tileY);
        }
        return tiles;
    }

    private Raster getTileNonSync(RenderedImage owner, int tileX, int tileY) {
        Object key = MemoryTile.hashKey(owner, tileX, tileY);
        MemoryTile ct = cache.get(key);
        Raster tile = null;
        // <NEW>
        if (ct == null) {
            ct = swapSpace.restoreTile(owner, tileX, tileY);
            if (ct != null) {
                if (!addTileNonSync(ct)) {
                    return ct.getTile();
                }
            }
        }
        // </NEW>
        if (ct == null) {
            missCount++;
        } else {
            tile = ct.getTile();
            // Update last-access time. (update() inlined for performance)
            ct.timeStamp = timeStamp++;

            if (ct != first) {
                // Bring this tile to the beginning of the list.
                if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = first;

                first.previous = ct;
                first = ct;
            }

            hitCount++;

            if (diagnostics) {
                ct.action = UPDATE_FROM_GETTILE;
                setChanged();
                notifyObservers(ct);
            }
        }
        return tile;
    }

    /**
     * Removes -ALL- tiles from the cache.
     */
    public synchronized void flush() {
        //
        // It is necessary to clear all the elements
        // from the old cache in order to remove dangling
        // references, due to the linked list.  In other
        // words, it is possible to reache the object
        // through 2 paths so the object does not
        // become weakly reachable until the reference
        // to it in the hash map is null. It is not enough
        // to just set the object to null.
        //
        Enumeration keys = cache.keys();    // all keys in Hashtable

        // reset counters before diagnostics
        hitCount = 0;
        missCount = 0;

        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            MemoryTile ct = cache.remove(key);

            // recalculate memoryUsage only if tile is actually removed
            if (ct != null) {
                memoryUsage -= ct.tileSize;
                tileCount--;

                if (ct == first) {
                    if (ct == last) {
                        first = null;  // only one tile in the list
                        last = null;
                    } else {
                        first = ct.next;
                        first.previous = null;
                    }
                } else if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = null;

                // diagnostics
                if (diagnostics) {
                    ct.action = REMOVE_FROM_FLUSH;
                    setChanged();
                    notifyObservers(ct);
                }
            }
        }

        if (memoryCapacity > 0) {
            cache = new Hashtable<Object, MemoryTile>(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);
        }

        if (cacheSortedSet != null) {
            cacheSortedSet.clear();
            cacheSortedSet = createSortedSet();
        }

        // force reset after diagnostics
        tileCount = 0;
        timeStamp = 0;
        memoryUsage = 0;

        // no System.gc() here, it's too slow and may occur anyway.
    }

    /**
     * Returns the cache's tile capacity.
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  This method always returns 0.
     */
    public int getTileCapacity() {
        return 0;
    }

    /**
     * Sets the cache's tile capacity to the desired number of tiles.
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  The cache size is limited by the memory
     * capacity only.  This method does nothing and has no effect on
     * the cache.
     *
     * @param tileCapacity The desired tile capacity for this cache
     *                     in number of tiles.
     */
    public void setTileCapacity(int tileCapacity) {
    }

    /**
     * Returns the cache's memory capacity in bytes.
     */
    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    /**
     * Sets the cache's memory capacity to the desired number of bytes.
     * If the new memory capacity is smaller than the amount of memory
     * currently being used by this cache, tiles are removed from the
     * cache until the memory usage is less than the specified memory
     * capacity.
     *
     * @param memoryCapacity The desired memory capacity for this cache
     *                       in bytes.
     * @throws IllegalArgumentException If <code>memoryCapacity</code>
     *                                  is less than 0.
     */
    public void setMemoryCapacity(long memoryCapacity) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException("memoryCapacity < 0");
        } else if (memoryCapacity == 0) {
            flush();
        }

        this.memoryCapacity = memoryCapacity;

        if (memoryUsage > memoryCapacity) {
            memoryControl();
        }
    }

    /**
     * Enable Tile Monitoring and Diagnostics
     */
    public void enableDiagnostics() {
        diagnostics = true;
    }

    /**
     * Turn off diagnostic notification
     */
    public void disableDiagnostics() {
        diagnostics = false;
    }

    public long getCacheTileCount() {
        return tileCount;
    }

    public long getCacheMemoryUsed() {
        return memoryUsage;
    }

    public long getCacheHitCount() {
        return hitCount;
    }

    public long getCacheMissCount() {
        return missCount;
    }

    /**
     * Reset hit and miss counters.
     *
     * @since 1.1
     */
    public void resetCounts() {
        hitCount = 0;
        missCount = 0;
    }

    /**
     * Set the memory threshold value.
     *
     * @since 1.1
     */
    public void setMemoryThreshold(float mt) {
        if (mt < 0.0F || mt > 1.0F) {
            throw new IllegalArgumentException("mt < 0.0F || mt > 1.0F");
        } else {
            memoryThreshold = mt;
            memoryControl();
        }
    }

    /**
     * Returns the current <code>memoryThreshold</code>.
     *
     * @since 1.1
     */
    public float getMemoryThreshold() {
        return memoryThreshold;
    }

    /**
     * Returns a string representation of the class object.
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
                ": memoryCapacity = " + Long.toHexString(memoryCapacity) +
                " memoryUsage = " + Long.toHexString(memoryUsage) +
                " #tilesInCache = " + Integer.toString(cache.size());
    }

    /**
     * @return the <code>Object</code> that represents the actual cache.
     */
    public Object getCachedObject() {
        return cache;
    }

    /**
     * Removes tiles from the cache based on their last-access time
     * (old to new) until the memory usage is memoryThreshold % of that of the
     * memory capacity.
     */
    public synchronized void memoryControl() {
        if (cacheSortedSet == null) {
            standard_memory_control();
        } else {
            custom_memory_control();
        }
    }

    // time stamp based memory control (LRU)
    private void standard_memory_control() {
        long limit = (long) (memoryCapacity * memoryThreshold);

        while (memoryUsage > limit && last != null) {
            MemoryTile ct = cache.get(last.key);

            if (ct != null) {
                ct = cache.remove(last.key);
                // <NEW>
                swapSpace.storeTile(ct);
                // </NEW>

                memoryUsage -= last.tileSize;
                tileCount--;

                last = last.previous;

                if (last != null) {
                    last.next.previous = null;
                    last.next = null;
                } else {
                    first = null;
                }

                // diagnostics
                if (diagnostics) {
                    ct.action = REMOVE_FROM_MEMCON;
                    setChanged();
                    notifyObservers(ct);
                }
            }
        }
    }

    // comparator based memory control (TreeSet)
    private void custom_memory_control() {
        long limit = (long) (memoryCapacity * memoryThreshold);
        Iterator iter = cacheSortedSet.iterator();
        MemoryTile ct;

        while (iter.hasNext() && (memoryUsage > limit)) {
            ct = (MemoryTile) iter.next();

            memoryUsage -= ct.tileSize;
            tileCount--;

            // remove from sorted set
            try {
                iter.remove();
            } catch (ConcurrentModificationException e) {
                ImagingListener listener =
                        ImageUtil.getImagingListener((RenderingHints) null);
                listener.errorOccurred("Concurrent modification of tile list.",
                                       e, this, false);
//                e.printStackTrace();
            }

            // remove tile from the linked list
            if (ct == first) {
                if (ct == last) {
                    first = null;
                    last = null;
                } else {
                    first = ct.next;

                    if (first != null) {
                        first.previous = null;
                        first.next = ct.next.next;
                    }
                }
            } else if (ct == last) {
                last = ct.previous;

                if (last != null) {
                    last.next = null;
                    last.previous = ct.previous.previous;
                }
            } else {
                MemoryTile ptr = first.next;

                while (ptr != null) {

                    if (ptr == ct) {
                        if (ptr.previous != null) {
                            ptr.previous.next = ptr.next;
                        }

                        if (ptr.next != null) {
                            ptr.next.previous = ptr.previous;
                        }

                        break;
                    }

                    ptr = ptr.next;
                }
            }

            // remove reference in the hashtable
            cache.remove(ct.key);

            // <NEW>
            swapSpace.storeTile(ct);
            // </NEW>

            // diagnostics
            if (diagnostics) {
                ct.action = REMOVE_FROM_MEMCON;
                setChanged();
                notifyObservers(ct);
            }
        }

        // If the custom memory control didn't release sufficient
        // number of tiles to satisfy the memory limit, fallback
        // to the standard memory controller.
        if (memoryUsage > limit) {
            standard_memory_control();
        }
    }

    /**
     * The <code>Comparator</code> is used to produce an
     * ordered list of tiles based on a user defined
     * compute cost or priority metric.  This determines
     * which tiles are subject to "ordered" removal
     * during a memory control operation.
     *
     * @since 1.1
     */
    public synchronized void setTileComparator(Comparator c) {
        comparator = c;

        if (comparator == null) {
            // turn of comparator
            if (cacheSortedSet != null) {
                cacheSortedSet.clear();
                cacheSortedSet = null;
            }
        } else {
            // copy tiles from hashtable to sorted tree set
            cacheSortedSet = createSortedSet();

            Enumeration keys = cache.keys();

            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                MemoryTile ct = cache.get(key);
                cacheSortedSet.add(ct);
            }
        }
    }

    /**
     * Return the current comparator
     *
     * @since 1.1
     */
    public Comparator getTileComparator() {
        return comparator;
    }

    // test
    public void dump() {

        System.out.println("first = " + first);
        System.out.println("last  = " + last);

        Iterator iter = cacheSortedSet.iterator();
        int k = 0;

        while (iter.hasNext()) {
            MemoryTile ct = (MemoryTile) iter.next();
            System.out.println(k++);
            System.out.println(ct);
        }
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
                ImageUtil.getImagingListener((RenderingHints) null);
        listener.errorOccurred(message, e, this, false);
    }

    SortedSet<MemoryTile> createSortedSet() {
        // noinspection unchecked
        return Collections.synchronizedSortedSet(new TreeSet<MemoryTile>(comparator));
    }
}
