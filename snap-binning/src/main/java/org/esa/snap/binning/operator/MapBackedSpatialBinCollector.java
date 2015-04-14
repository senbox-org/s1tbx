/*
 *
 *  * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.binning.operator;

import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.SpatialBin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link SpatialBinCollector} which simply stores the consumed {@link SpatialBin spatial bins} in a map.
 * This means that all data is kept in memory. There are other implementations which consume less memory.
 *
 * @see GeneralSpatialBinCollector
 * @see FileBackedSpatialBinCollector
 */
class MapBackedSpatialBinCollector implements SpatialBinCollector {

    // Note, we use a sorted map in order to sort entries on-the-fly
    private final SortedMap<Long, List<SpatialBin>> spatialBinMap = new TreeMap<Long, List<SpatialBin>>();

    private AtomicBoolean consumingCompleted;

    MapBackedSpatialBinCollector() {
        consumingCompleted = new AtomicBoolean(false);
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() {
        return new SortedMapWrappingSpatialBinCollection(spatialBinMap);
    }

    @Override
    public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {
        if (consumingCompleted.get()) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }

        synchronized (spatialBinMap) {
            for (SpatialBin spatialBin : spatialBins) {
                final long spatialBinIndex = spatialBin.getIndex();
                List<SpatialBin> spatialBinList = spatialBinMap.get(spatialBinIndex);
                if (spatialBinList == null) {
                    spatialBinList = new ArrayList<SpatialBin>();
                    spatialBinMap.put(spatialBinIndex, spatialBinList);
                }
                spatialBinList.add(spatialBin);
            }
        }
    }

    @Override
    public void consumingCompleted() {
        consumingCompleted.set(true);
    }

    @Override
    public void close() throws IOException {
        clearMap();
    }

    void clearMap() {
        spatialBinMap.clear();
    }

    private class SortedMapWrappingSpatialBinCollection implements SpatialBinCollection {

        private SortedMap<Long, List<SpatialBin>> map;

        private SortedMapWrappingSpatialBinCollection(SortedMap<Long, List<SpatialBin>> spatialBinMap) {
            this.map = spatialBinMap;
        }

        @Override
        public Iterable<List<SpatialBin>> getBinCollection() {
            return new Iterable<List<SpatialBin>>() {
                @Override
                public Iterator<List<SpatialBin>> iterator() {
                    return map.values().iterator();
                }
            };
        }

        @Override
        public long size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

    }
}
