package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;

/**
 * A cache for tiles.
 */
public interface TileCache {

    /**
     * Gets the currently used memory in bytes.
     *
     * @return the current memory in bytes.
     */
    long getCurrentMemory();

    /**
     * Gets the minimum size for which tiles should be cached.
     *
     * @return the current minimum tile size.
     */
    long getMinimumTileSize();

    /**
     * Sets the minimum size for tiles. Tiles smaller than this are
     * not cached.
     *
     * @param minimumTileSize the minimum tile size
     */
    void setMinimumTileSize(long minimumTileSize);

    /**
     * Gets the memory capacity in bytes.
     *
     * @return the memory capacity in bytes.
     */
    long getMemoryCapacity();

    /**
     * Sets the memory capacity in bytes.
     *
     * @param memoryCapacity the memory capacity in bytes.
     */
    void setMemoryCapacity(long memoryCapacity);

    /**
     * Gets the memory load factor.
     * If memory usage is greater than {@code loadFactor * capacity<}, tiles will be released.
     *
     * @return the memory load factor.
     */
    double getMemoryLoadFactor();

    /**
     * Sets the memory load factor.
     * If memory usage is greater than {@code loadFactor * capacity<}, tiles will be released.
     *
     * @param memoryLoadFactor the memory load factor.
     */
    void setMemoryLoadFactor(double memoryLoadFactor);

    /**
     * Creates a new tile.
     *
     * @param rasterDataNode the raster data node
     * @param tileRectangle  the tile rectangle
     * @param dataBuffer     the data buffer, may be {@code null}
     *
     * @return the new tile
     */
    Tile createTile(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer);

    /**
     * Gets an existing tile.
     *
     * @param rasterDataNode the raster data node
     * @param tileRectangle  the tile rectangle
     *
     * @return the existing tile, or {@code null}
     */
    Tile getTile(RasterDataNode rasterDataNode, Rectangle tileRectangle);

    /**
     * Gets all existing tiles for the given {@link RasterDataNode}.
     *
     * @param rasterDataNode the raster data node
     *
     * @return the existing tiles, or {@code null}
     */
    Tile[] getTiles(RasterDataNode rasterDataNode);

    /**
     * Releases the given tile.
     *
     * @param tile the tile to be released
     */
    void releaseTile(Tile tile);

    /**
     * Cleans the cache.
     */
    void clean();

}
