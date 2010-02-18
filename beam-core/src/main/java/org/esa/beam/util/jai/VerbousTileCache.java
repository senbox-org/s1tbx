package org.esa.beam.util.jai;

import com.sun.media.jai.util.CacheDiagnostics;

import javax.media.jai.TileCache;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Comparator;

public class VerbousTileCache implements TileCache {
    private final TileCache tileCache;
    private CacheDiagnostics cacheDiagnostics;
    String indent = "";

    static boolean verbous;

    public static boolean isVerbous() {
        return verbous;
    }

    public static void setVerbous(boolean verbous) {
        VerbousTileCache.verbous = verbous;
    }

    public VerbousTileCache(TileCache tileCache) {
        this.tileCache = tileCache;
        if (tileCache instanceof CacheDiagnostics) {
            cacheDiagnostics = (CacheDiagnostics) tileCache;
            cacheDiagnostics.enableDiagnostics();
        }
    }

    public void add(RenderedImage renderedImage, int tileX, int tileY, Raster tile) {
        tileCache.add(renderedImage, tileX, tileY, tile);
        trace("add", renderedImage, tile, getTilePos(tileX, tileY));
    }


    public void add(RenderedImage renderedImage, int tileX, int tileY, Raster tile, Object o) {
        tileCache.add(renderedImage, tileX, tileY, tile, o);
        trace("add-2", renderedImage, tile, getTilePos(tileX, tileY));
    }

    public void remove(RenderedImage renderedImage, int tileX, int tileY) {
        tileCache.remove(renderedImage, tileX, tileY);
        trace("remove", renderedImage, null, getTilePos(tileX, tileY));
    }

    public Raster getTile(RenderedImage renderedImage, int tileX, int tileY) {
        Raster tile = tileCache.getTile(renderedImage, tileX, tileY);
        trace("getTile", renderedImage, tile, getTilePos(tileX, tileY));
        return tile;
    }

    private static String getTilePos(int tileX, int tileY) {
        return "(" + tileX + "," + tileY + ")";
    }

    public Raster[] getTiles(RenderedImage renderedImage) {
        Raster[] tiles = tileCache.getTiles(renderedImage);
        trace("getTiles", renderedImage, null, "n.a.");
        return tiles;
    }

    public void removeTiles(RenderedImage renderedImage) {
        tileCache.removeTiles(renderedImage);
        trace("removeTiles", renderedImage, null, "n.a.");
    }

    public void addTiles(RenderedImage renderedImage, Point[] points, Raster[] rasters, Object o) {
        tileCache.addTiles(renderedImage, points, rasters, o);
        trace("addTiles", renderedImage, null, "n.a.");
    }

    public Raster[] getTiles(RenderedImage renderedImage, Point[] points) {
        Raster[] tiles = tileCache.getTiles(renderedImage, points);
        trace("getTiles", renderedImage, null, "n.a.");
        return tiles;
    }

    public void flush() {
        trace("flush (start)", null, null, "n.a.");
        tileCache.flush();
        trace("flush (end)", null, null, "n.a.");
    }

    public void memoryControl() {
        trace("memoryControl (start)", null, null, "n.a.");
        tileCache.memoryControl();
        trace("memoryControl (end)", null, null, "n.a.");
    }

    public void setTileCapacity(int i) {
        tileCache.setTileCapacity(i);
    }

    public int getTileCapacity() {
        return tileCache.getTileCapacity();
    }

    public void setMemoryCapacity(long l) {
        tileCache.setMemoryCapacity(l);
    }

    public long getMemoryCapacity() {
        return tileCache.getMemoryCapacity();
    }

    public void setMemoryThreshold(float v) {
        tileCache.setMemoryThreshold(v);
    }

    public float getMemoryThreshold() {
        return tileCache.getMemoryThreshold();
    }

    public void setTileComparator(Comparator comparator) {
        tileCache.setTileComparator(comparator);
    }

    public Comparator getTileComparator() {
        return tileCache.getTileComparator();
    }

    private void trace(String method, RenderedImage image, Raster tile, String tilePos) {
        if (verbous) {
            println("JAI TileCache Diagnostics: ======================================");
            println("  cache   = " + tileCache);
            println("  method  = " + method);
            println("  image   = " + image);
            println("  tilePos = " + tilePos);
            String tileString = "  tile    = " + tile;
            String dimString = "";
            if (tile != null) {
                dimString = " [width = " + tile.getWidth() + " height = " + tile.getHeight() + "]";
            }
            println(tileString + dimString);
            if (cacheDiagnostics != null) {
                println("  cacheTileCount  = " + cacheDiagnostics.getCacheTileCount());
                println("  cacheHitCount   = " + cacheDiagnostics.getCacheHitCount());
                println("  cacheMissCount  = " + cacheDiagnostics.getCacheMissCount());
                println("  cacheMemoryUsed = " + cacheDiagnostics.getCacheMemoryUsed());
            }
        } else {
            println(method + " : " + tilePos + " " + image);
        }
    }

    private void println(String x) {
        // Uncomment for testing
        //System.out.println(x);
    }
}
