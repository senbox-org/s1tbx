package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
* An implementation of {@link SpatialBinStore} which simply stores the consumed {@link SpatialBin spatial bins} in a map.
*/
class SimpleSpatialBinStore implements SpatialBinStore {

    // Note, we use a sorted map in order to sort entries on-the-fly
    final private SortedMap<Long, List<SpatialBin>> spatialBinMap = new TreeMap<Long, List<SpatialBin>>();

    @Override
    public SortedMap<Long, List<SpatialBin>> getSpatialBinMap() {
        return spatialBinMap;
    }

    @Override
    public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {

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

    @Override
    public void consumingCompleted() {
    }
}
