package org.esa.snap.binning.reader;

import org.esa.snap.core.datamodel.Band;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class NcArrayCache {

    private final Map<Band, CacheEntry> cache;
    private final Timer timer;

    NcArrayCache() {
        cache = new HashMap<Band, CacheEntry>();
        timer = new Timer();
        TimerTask clearCacheTask = new TimerTask() {
            @Override
            public void run() {
                final long expired = System.currentTimeMillis() - 2000;
                synchronized (cache) {
                    Iterator<Map.Entry<Band, CacheEntry>> iterator = cache.entrySet().iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().getValue().getLastAccess() < expired) {
                            iterator.remove();
                        }
                    }
                }
            }
        };
        timer.schedule(clearCacheTask, 2000, 2000);
    }

    void dispose() {
        cache.clear();
        timer.cancel();
    }

    public CacheEntry get(Band destBand) {
        return cache.get(destBand);
    }

    public void put(Band destBand, CacheEntry cacheEntry) {
        cache.put(destBand, cacheEntry);
    }
}
