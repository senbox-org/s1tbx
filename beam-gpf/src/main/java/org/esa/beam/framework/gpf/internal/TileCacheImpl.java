package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.TileCache;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TileCacheImpl implements TileCache {

    private static final long MEGAS = 1024 * 1024;

    private static final long DEFAULT_MINIMUM_TILE_SIZE = 128;
    private static final long DEFAULT_MEMORY_CAPACITY = 256 * MEGAS;
    private static final double DEFAULT_MEMORY_LOAD_FACTOR = 0.75;

    private final Map<RasterDataNode, List<Tile>> cache;
    private long currentMemory;
    private long minimumTileSize;
    private long memoryCapacity;
    private double memoryLoadFactor;
    private Logger logger;

    public TileCacheImpl() {
        cache = new WeakHashMap<RasterDataNode, List<Tile>>(1024);
        currentMemory = 0;
        minimumTileSize = DEFAULT_MINIMUM_TILE_SIZE;
        memoryCapacity = DEFAULT_MEMORY_CAPACITY;
        memoryLoadFactor = DEFAULT_MEMORY_LOAD_FACTOR;
        logger = Logger.getAnonymousLogger();
    }

    public long getCurrentMemory() {
        return currentMemory;
    }

	public long getMinimumTileSize() {
		return minimumTileSize;
	}

	public void setMinimumTileSize(long minimumTileSize) {
		this.minimumTileSize = minimumTileSize;
	}

	public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public double getMemoryLoadFactor() {
        return memoryLoadFactor;
    }

    public void setMemoryLoadFactor(double memoryLoadFactor) {
        this.memoryLoadFactor = memoryLoadFactor;
    }

    public Tile getTile(RasterDataNode rasterDataNode, Rectangle tileRectangle) {
        List<Tile> tiles = cache.get(rasterDataNode);
        if (tiles != null) {
            for (Tile tile : tiles) {
                if (tile.getRectangle().equals(tileRectangle)) {
                    tile.incQueryCount();
                    traceTile(tile, "Tile queried");
                    return tile;
                }
            }
        }
        return null;
    }

    public Tile[] getTiles(RasterDataNode rasterDataNode) {
        List<Tile> tiles = cache.get(rasterDataNode);
        if(tiles != null) {
            return tiles.toArray(new Tile[tiles.size()]);
        }
        return new Tile[0];
    }

    public Tile createTile(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer) {
    	Tile tile = Tile.createTile(rasterDataNode, tileRectangle, dataBuffer);
        long tileSize = tile.getMemorySize();
        if (!canStore(tileSize)) {
            releaseTiles();
        }
        if (canStore(tileSize)) {
            List<Tile> tiles = cache.get(rasterDataNode);
            if (tiles == null) {
                tiles = new LinkedList<Tile>();
                cache.put(rasterDataNode, tiles);
            }
            tiles.add(tile);
            currentMemory += tileSize;
            traceTile(tile, "Tile created");
        }
        return tile;
    }

    public void releaseTile(Tile tile) {
        List<Tile> tiles = cache.get(tile.getRaster().getRasterDataNode());
        if (tiles != null && tiles.remove(tile)) {
            currentMemory -= tile.getMemorySize();
            traceTile(tile, "Tile released");
        }
    }

    public void clean() {
        cache.clear();
        currentMemory = 0;
    }

    private void releaseTiles() {
        ArrayList<Tile> list = new ArrayList<Tile>();
        Set<Map.Entry<RasterDataNode, List<Tile>>> entries = cache.entrySet();
        for (Map.Entry<RasterDataNode, List<Tile>> entry : entries) {
            List<Tile> value = entry.getValue();
            for (Tile tile : value) {
                list.add(tile);
            }
        }
        Tile[] tiles = list.toArray(new Tile[list.size()]);

        // todo - extract the following method as one possible TileReleaseStrategy.releaseTiles()
        releaseTiles(tiles);
    }

    private void releaseTiles(Tile[] tiles) {
        // sort array in descending order: query count, memory size, time stamp
        Arrays.sort(tiles, new Comparator<Tile>() {
            public int compare(Tile tile1, Tile tile2) {
                int delta;
//                delta = (tile1.getQueryCount() - tile2.getQueryCount()) / 10;
//                if (delta != 0) {
//                    // lowest query count first
//                    return -delta;
//                }
//                delta = (int) (tile1.getMemorySize() - tile2.getMemorySize());
//                if (delta != 0) {
//                    // higest memory size first
//                    return +delta;
//                }
                delta = (int) ((tile1.getTimeStamp() - tile2.getTimeStamp()) / 1000);
                if (delta != 0) {
                    // lowest time stamp first
                    return delta;
                }
                return 0;
            }
        });

        for (Tile tile : tiles) {
            if (currentMemory > Math.round(memoryLoadFactor * memoryLoadFactor * memoryCapacity)) {
                releaseTile(tile);
            }
        }
    }

    private boolean canStore(long tileSize) {
        return tileSize > minimumTileSize
               && currentMemory + tileSize <= Math.round(memoryLoadFactor * memoryCapacity);
    }

    @Override
    public String toString() {
        long currentMegas = getCurrentMemory() / MEGAS;
        StringBuilder sb = new StringBuilder("TileCache[");
        sb.append("mem=");
        sb.append(currentMegas);
        sb.append("MB");
        sb.append("]");
        return sb.toString();
    }

    private void traceTile(Tile tile, String message) {
        if (logger.isLoggable(Level.FINEST)) {
             StringBuilder sb = new StringBuilder(toString());
            sb.append(": ");
            sb.append(message);
            sb.append(": ");
            sb.append(tile);
            logger.log(Level.FINEST, sb.toString());
        }
    }
}
