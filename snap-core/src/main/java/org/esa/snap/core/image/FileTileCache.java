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

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.CachedTile;
import javax.media.jai.TileCache;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A class implementing a caching mechanism for image tiles.
 * <p> <code>TileCache</code> provides a mechanism by which an
 * <code>OpImage</code> may cache its computed tiles.  There may be
 * multiple <code>TileCache</code>s used in an application up to the
 * point of having a different <code>TileCache</code> for each
 * <code>OpImage</code>.
 * <p> The <code>TileCache</code> used for a particular <code>OpImage</code>
 * is derived from the <code>RenderingHints</code> assigned to the
 * associated imaging chain node.  If the node is constructed using
 * <code>JAI.create()</code> and no <code>TileCache</code> is specified
 * in the <code>RenderingHints</code> parameter, then one is derived
 * from the <code>RenderingHints</code> associated with the instance of the
 * <code>JAI</code> class being used.
 * <p> In the Sun reference implementation, the cache size is limited by
 * the memory capacity, which is set to a default value at construction
 * or subsequently using the <code>setMemoryCapacity()</code> method.
 * The initial value may be obtained using <code>getMemoryCapacity()</code>.
 * The tile capacity is not used as different images may have very different
 * tile sizes so that this metric is not a particularly meaningful control
 * of memory resource consumption in general.
 *
 * @see javax.media.jai.JAI
 * @see javax.media.jai.RenderedOp
 * @see java.awt.RenderingHints
 */
public class FileTileCache implements TileCache {

    private final File cacheDir;
    private long memoryCapacity;
    private long memoryInUse;
    private float memoryThreshold;
    private Comparator<Object> tileComparator;
    private Map<TileId, CachedTileImpl> tileMap;
    private Map<RenderedImage, String> idMap;

    public FileTileCache(File cacheDir) {
        this.cacheDir = cacheDir;
        this.tileMap = new HashMap<TileId, CachedTileImpl>(1024);
        this.idMap = new WeakHashMap<RenderedImage, String>(128);

        this.memoryCapacity = 100L * (1024 * 1024);
        this.memoryThreshold = 0.75f;
    }

    /**
     * Adds a tile to the cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     * @param tile  A <code>Raster</code> containing the tile data.
     */
    public void add(RenderedImage owner, int tileX, int tileY, Raster tile) {
        add(owner, tileX, tileY, tile, null);
    }

    /**
     * Adds a tile to the cache with an associated compute cost
     *
     * @param owner           The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX           The X index of the tile in the owner's tile grid.
     * @param tileY           The Y index of the tile in the owner's tile grid.
     * @param tile            A <code>Raster</code> containing the tile data.
     * @param tileCacheMetric An <code>Object</code> as a tile metric.
     * @since JAI 1.1
     */
    public void add(RenderedImage owner, int tileX, int tileY, Raster tile, Object tileCacheMetric) {
        TileId tileId = createTileId(owner, tileX, tileY);
        synchronized (this) {
            CachedTileImpl cachedTile = tileMap.get(tileId);
            try {
                if (cachedTile != null) {
                    writeTile(cachedTile.file, tile);
                    cachedTile.tileTimeStamp = System.currentTimeMillis();
                } else {
                    cachedTile = new CachedTileImpl(tileId, tile, tileCacheMetric);
                    if (memoryInUse + cachedTile.tileSize > memoryThreshold) {
                        memoryControl();
                    }
                    if (memoryInUse + cachedTile.tileSize <= memoryThreshold) {
                        writeTile(cachedTile.file, tile);
                        tileMap.put(tileId, cachedTile);
                        memoryInUse += cachedTile.tileSize;
                    }
                }
            } catch (IOException e) {
                // todo - log warning
                cachedTile.file.delete();
            }
        }
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
     * @since JAI 1.1
     */
    public void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles, Object tileCacheMetric) {
        for (int i = 0; i < tileIndices.length; i++) {
            Point tileIndex = tileIndices[i];
            add(owner, tileIndex.x, tileIndex.y, tiles[i], tileCacheMetric);
        }
    }

    /**
     * Retrieves a tile.  Returns <code>null</code> if the tile is not
     * present in the cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     */
    public Raster getTile(RenderedImage owner, int tileX, int tileY) {
        TileId tileId = createTileId(owner, tileX, tileY);
        Raster tile = null;
        synchronized (this) {
            CachedTileImpl cachedTile = tileMap.get(tileId);
            if (cachedTile != null) {
                try {
                    DataBuffer dataBuffer = readTileData(cachedTile.file, cachedTile.sampleModel);
                    if (cachedTile.writable) {
                        tile = Raster.createWritableRaster(cachedTile.sampleModel, dataBuffer, cachedTile.location);
                    } else {
                        tile = Raster.createRaster(cachedTile.sampleModel, dataBuffer, cachedTile.location);
                    }
                    cachedTile.tileTimeStamp = System.currentTimeMillis();
                } catch (IOException e) {
                    // todo - log warning
                }
            }
        }
        return tile;
    }


    /**
     * Retrieves an array of all tiles in the cache which are owned by the
     * specified image.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cache.
     * @since JAI 1.1
     */
    public Raster[] getTiles(RenderedImage owner) {
        return getTiles(owner, getTileIndices(owner));
    }

    /**
     * Returns an array of tile <code>Raster</code>s from the cache.
     * Any or all of the elements of the returned array may be
     * <code>null</code> if the corresponding tile is not in the cache.
     * The length of the returned array must be the same as that of the
     * parameter array and the <i>i</i>th <code>Raster</code> in the
     * returned array must correspond to the <i>i</i>th tile index in
     * the parameter array.
     *
     * @param owner       The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices An array of <code>Point</code>s containing the
     *                    <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @since JAI 1.1
     */
    public Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {
        Raster[] tiles = new Raster[tileIndices.length];
        for (int i = 0; i < tiles.length; i++) {
            Point tileIndex = tileIndices[i];
            tiles[i] = getTile(owner, tileIndex.x, tileIndex.y);
        }
        return tiles;
    }

    /**
     * Advises the cache that all tiles associated with a given image
     * are no longer needed.  It is legal to implement this method as
     * a no-op.
     *
     * @param owner The <code>RenderedImage</code> owner of the tiles
     *              to be removed.
     */
    public void removeTiles(RenderedImage owner) {
        Point[] tileIndices = getTileIndices(owner);
        for (Point tileIndex : tileIndices) {
            remove(owner, tileIndex.x, tileIndex.y);
        }
    }

    /**
     * Advises the cache that a tile is no longer needed.  It is legal
     * to implement this method as a no-op.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     */
    public void remove(RenderedImage owner, int tileX, int tileY) {
        removeTile(createTileId(owner, tileX, tileY));
    }

    private synchronized TileId createTileId(RenderedImage owner, int tileX, int tileY) {
        return new TileId(owner, tileX, tileY);
    }

    /**
     * Advises the cache that all of its tiles may be discarded.  It
     * is legal to implement this method as a no-op.
     */
    public synchronized void flush() {
        TileId[] tileIds;
        tileIds = tileMap.keySet().toArray(new TileId[tileMap.size()]);
        for (TileId tileId : tileIds) {
            removeTile(tileId);
        }
        tileMap.clear();
        idMap.clear();
    }

    /**
     * Advises the cache that some of its tiles may be discarded.  It
     * is legal to implement this method as a no-op.
     *
     * @since JAI 1.1
     */
    public synchronized void memoryControl() {
        CachedTileImpl[] tiles = tileMap.values().toArray(new CachedTileImpl[tileMap.size()]);
        Arrays.sort(tiles, createTileComparator());
        for (CachedTileImpl tile : tiles) {
            if (memoryInUse <= memoryThreshold * memoryCapacity) {
                break;
            }
            removeTile(tile.tileId);
        }
    }

    /**
     * Sets the tile capacity to a desired number of tiles.
     * If the capacity is smaller than the current capacity,
     * tiles are flushed from the cache.  It is legal to
     * implement this method as a no-op.
     *
     * @param tileCapacity The new capacity, in tiles.
     * @deprecated as of JAI 1.1.
     */
    public synchronized void setTileCapacity(int tileCapacity) {
    }

    /**
     * Returns the tile capacity in tiles.  It is legal to
     * implement this method as a no-op which should be
     * signaled by returning zero.
     *
     * @deprecated as of JAI 1.1.
     */
    public synchronized int getTileCapacity() {
        return 0;
    }

    /**
     * Sets the memory capacity to a desired number of bytes.
     * If the memory capacity is smaller than the amount of
     * memory currently used by the cache, tiles are flushed
     * until the <code>TileCache</code>'s memory usage is less than
     * <code>memoryCapacity</code>.
     *
     * @param memoryCapacity The new capacity, in bytes.
     */
    public synchronized void setMemoryCapacity(long memoryCapacity) {
        long oldCapacity = this.memoryCapacity;
        this.memoryCapacity = memoryCapacity;
        if (this.memoryCapacity < oldCapacity) {
            memoryControl();
        }
    }

    /**
     * Returns the memory capacity in bytes.
     */
    public synchronized long getMemoryCapacity() {
        return memoryCapacity;
    }

    /**
     * Sets the <code>memoryThreshold</code> value to a floating
     * point number that ranges from 0.0 to 1.0.
     * When the cache memory is full, the memory
     * usage will be reduced to this fraction of
     * the total cache memory capacity.  For example,
     * a value of .75 will cause 25% of the memory
     * to be cleared, while retaining 75%.
     *
     * @param memoryThreshold Retained fraction of memory
     * @throws IllegalArgumentException if the memoryThreshold
     *                                  is less than 0.0 or greater than 1.0
     * @since JAI 1.1
     */
    public synchronized void setMemoryThreshold(float memoryThreshold) {
        float oldThreshold = this.memoryThreshold;
        this.memoryThreshold = memoryThreshold;
        if (this.memoryThreshold < oldThreshold) {
            memoryControl();
        }
    }

    /**
     * Returns the memory threshold, which is the fractional
     * amount of cache memory to retain during tile removal.
     *
     * @since JAI 1.1
     */
    public synchronized float getMemoryThreshold() {
        return memoryCapacity;
    }

    /**
     * Sets a <code>Comparator</code> which imposes an order on the
     * <code>CachedTile</code>s stored in the <code>TileCache</code>.
     * This ordering is used in <code>memoryControl()</code> to determine
     * the sequence in which tiles will be removed from the
     * <code>TileCache</code> so as to reduce the memory to the level given
     * by the memory threshold.  The <code>Object</code>s passed to the
     * <code>compare()</code> method of the <code>Comparator</code> will
     * be instances of <code>CachedTile</code>.  <code>CachedTile</code>s
     * will be removed from the <code>TileCache</code> in the ascending
     * order imposed by this <code>Comparator</code>.  If no
     * <code>Comparator</code> is currently set, the <code>TileCache</code>
     * should use an implementation-dependent default ordering.  In the
     * Sun Microsystems, Inc., implementation of <code>TileCache</code>,
     * this ordering is the <u>l</u>east <u>r</u>ecently <u>u</u>sed
     * ordering, i.e., the tiles least recently used will be removed first
     * by <code>memoryControl()</code>.
     *
     * @param comparator A <code>Comparator</code> which orders the
     *                   <code>CachedTile</code>s stored by the <code>TileCache</code>;
     *                   if <code>null</code> an implementation-dependent algorithm
     *                   will be used.
     * @since JAI 1.1
     */
    public synchronized void setTileComparator(Comparator comparator) {
        this.tileComparator = comparator;
    }

    /**
     * Returns the <code>Comparator</code> currently set for use in ordering
     * the <code>CachedTile</code>s stored by the <code>TileCache</code>.
     *
     * @return The tile <code>Comparator</code> or <code>null</code> if the
     *         implementation-dependent ordering algorithm is being used.
     * @since JAI 1.1
     */
    public synchronized Comparator getTileComparator() {
        return tileComparator;
    }

    /////////////////////////////////////////////////////////////////////////
    // Implementation Helpers

    private synchronized void removeTile(TileId tileId) {
        CachedTileImpl cachedTile = tileMap.get(tileId);
        if (cachedTile != null) {
            cachedTile.file.delete();
        }
        tileMap.remove(tileId);
        RenderedImage image = tileId.owner.get();
        if (image != null) {
            idMap.remove(image);
        }
    }

    static Point[] getTileIndices(RenderedImage owner) {
        int numXTiles = owner.getNumXTiles();
        int numYTiles = owner.getNumYTiles();
        Point[] tileIndices = new Point[numXTiles * numYTiles];
        for (int i = 0, y = 0; y < numYTiles; y++) {
            for (int x = 0; x < numXTiles; x++) {
                tileIndices[i++] = new Point(x, y);
            }
        }
        return tileIndices;
    }


    final String getImageId(RenderedImage owner) {
        String id = idMap.get(owner);
        if (id == null) {
            synchronized (this) {
                if (id == null) {
                    id = createImageId(owner, idMap.size());
                    idMap.put(owner, id);
                }
            }
        }
        return id;
    }

    static String createImageId(RenderedImage owner, int index) {
        return  owner.getClass().getSimpleName()
                + "-" + Long.toHexString(System.nanoTime())
                + "-" + Integer.toHexString(index)
                + "-" + Integer.toHexString(owner.hashCode());
    }

    final Comparator<CachedTileImpl> createTileComparator() {
        return tileComparator != null ? new TileCacheMetricComparator(tileComparator) : new DefaultTileComparator();
    }

    static DataBuffer readTileData(File file, SampleModel sampleModel) throws IOException {
        ImageInputStream stream = new FileImageInputStream(file);
        try {
            return readTile(stream, sampleModel);
        } finally {
            stream.close();
        }
    }

    static void writeTile(File file, Raster tile) throws IOException {
        ImageOutputStream stream = new FileImageOutputStream(file);
        try {
            writeTile(stream, tile);
        } finally {
            stream.close();
        }
    }

    static DataBuffer readTile(ImageInputStream stream, SampleModel sampleModel) throws IOException {
        int numDataElements = sampleModel.getNumDataElements();
        int dataType = sampleModel.getDataType();
        if (dataType == DataBuffer.TYPE_BYTE) {
            byte[] data = new byte[numDataElements];
            stream.readFully(data, 0, data.length);
            return new DataBufferByte(data, data.length);
        } else if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
            short[] data = new short[numDataElements];
            stream.readFully(data, 0, data.length);
            return new DataBufferShort(data, data.length);
        } else if (dataType == DataBuffer.TYPE_INT) {
            int[] data = new int[numDataElements];
            stream.readFully(data, 0, data.length);
            return new DataBufferInt(data, data.length);
        } else if (dataType == DataBuffer.TYPE_FLOAT) {
            float[] data = new float[numDataElements];
            stream.readFully(data, 0, data.length);
            return new DataBufferFloat(data, data.length);
        } else if (dataType == DataBuffer.TYPE_DOUBLE) {
            double[] data = new double[numDataElements];
            stream.readFully(data, 0, data.length);
            return new DataBufferDouble(data, data.length);
        } else {
            throw new IllegalStateException();
        }
    }

    static void writeTile(ImageOutputStream stream, Raster tile) throws IOException {
        if (tile.getSampleModel().getNumDataElements() != tile.getDataBuffer().getSize()) {
            // todo - log error
            throw new IllegalStateException();
        }
        DataBuffer dataBuffer = tile.getDataBuffer();
        if (dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte) dataBuffer).getData();
            stream.write(data, dataBuffer.getOffset(), dataBuffer.getSize());
        } else if (dataBuffer instanceof DataBufferShort) {
            short[] data = ((DataBufferShort) dataBuffer).getData();
            stream.writeShorts(data, dataBuffer.getOffset(), dataBuffer.getSize());
        } else if (dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt) dataBuffer).getData();
            stream.writeInts(data, dataBuffer.getOffset(), dataBuffer.getSize());
        } else if (dataBuffer instanceof DataBufferFloat) {
            float[] data = ((DataBufferFloat) dataBuffer).getData();
            stream.writeFloats(data, dataBuffer.getOffset(), dataBuffer.getSize());
        } else if (dataBuffer instanceof DataBufferDouble) {
            double[] data = ((DataBufferDouble) dataBuffer).getData();
            stream.writeDoubles(data, dataBuffer.getOffset(), dataBuffer.getSize());
        } else {
            throw new IllegalStateException();
        }
    }

    private static int getHashCode(RenderedImage owner, int tileX, int tileY) {
        int result;
        result = owner.hashCode();
        result = 31 * result + tileY * owner.getNumXTiles()  + tileX;
        return result;
    }

    final static class TileCacheMetricComparator implements Comparator<CachedTileImpl> {
        private Comparator<Object> tileComparator;

        TileCacheMetricComparator(Comparator<Object> tileComparator) {
            this.tileComparator = tileComparator;
        }

        public int compare(CachedTileImpl o1, CachedTileImpl o2) {
            return tileComparator.compare(o1.tileCacheMetric, o2.tileCacheMetric);
        }
    }

    final static class DefaultTileComparator implements Comparator<CachedTileImpl> {

        public int compare(CachedTileImpl o1, CachedTileImpl o2) {
                return (int) (o1.tileTimeStamp - o2.tileTimeStamp);
        }
    }

    final class TileId {
        final WeakReference<RenderedImage> owner;
        final int tileX;
        final int tileY;
        final int hash;

        TileId(RenderedImage owner, int tileX, int tileY) {
            this.owner = new WeakReference<RenderedImage>(owner);
            this.tileX = tileX;
            this.tileY = tileY;
            this.hash = getHashCode(owner, tileX, tileY);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            TileId tileId = (TileId) other;
            return owner.get() == tileId.owner.get()
                    && hash == tileId.hash
                    && tileX == tileId.tileX
                    && tileY == tileId.tileY;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    final class CachedTileImpl implements CachedTile {

        final TileId tileId;
        final File file;
        final Object tileCacheMetric;
        final SampleModel sampleModel;
        final long tileSize;
        final Point location;
        final boolean writable;
        long tileTimeStamp;

        CachedTileImpl(TileId tileId, Raster tile, Object tileCacheMetric) {
            this.tileId = tileId;
            this.file = new File(cacheDir, getImageId(tileId.owner.get()) + "-" + tileId.tileX + "-" + tileId.tileY);
            this.tileCacheMetric = tileCacheMetric;
            this.sampleModel = tile.getSampleModel();
            this.tileSize = sampleModel.getNumDataElements() * DataBuffer.getDataTypeSize(sampleModel.getTransferType());
            this.location = tile.getBounds().getLocation();
            this.writable = tile instanceof WritableRaster;
            this.tileTimeStamp = System.currentTimeMillis();
        }

        /**
         * Returns the image operation to which this
         * cached tile belongs.
         */
        public RenderedImage getOwner() {
            return tileId.owner.get();
        }

        /**
         * Returns the cached tile.
         */
        public Raster getTile() {
            return null;
        }

        /**
         * Returns a cost metric associated with the tile.
         * This value is used to determine which tiles get
         * removed from the cache.
         */
        public Object getTileCacheMetric() {
            return tileCacheMetric;
        }

        /**
         * Returns the time stamp of the cached tile.
         */
        public long getTileTimeStamp() {
            return tileTimeStamp;
        }

        /**
         * Returns the memory size of the cached tile
         */
        public long getTileSize() {
            return tileSize;
        }

        /**
         * Returns information about which method
         * triggered a notification event.
         */
        public int getAction() {
            return 0;
        }
    }

}
