package org.esa.snap.binning.reader;

import ucar.ma2.Array;

class CacheEntry {

    private final Array cachedData;
    private long lastAccess;

    CacheEntry(Array data) {
        cachedData = data;
        lastAccess = System.currentTimeMillis();
    }

    Array getData() {
        lastAccess = System.currentTimeMillis();
        return cachedData;
    }

    long getLastAccess() {
        return lastAccess;
    }
}
