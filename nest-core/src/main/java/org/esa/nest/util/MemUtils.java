package org.esa.nest.util;

//import org.jaitools.tilecache.DiskMemTileCache;

import javax.media.jai.JAI;

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
    }

    /**
     * tell tileCache that some old tiles can be removed
     */
    public static void tileCacheFreeOldTiles() {
        JAI.getDefaultInstance().getTileCache().memoryControl();
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
