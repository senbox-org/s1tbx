package org.esa.snap.binning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the {@link SpatialBinConsumer} interface that performs a temporal binning.
 */
class MySpatialBinConsumer implements SpatialBinConsumer {
    private final BinManager binManager;
    final Map<Long, TemporalBin> binMap;

    MySpatialBinConsumer(BinManager binManager) {
        this.binManager = binManager;
        this.binMap = new HashMap<Long, TemporalBin>();
    }

    @Override
    public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> sliceBins) {
        for (SpatialBin spatialBin : sliceBins) {
            TemporalBin temporalBin = binMap.get(spatialBin.getIndex());
            if (temporalBin == null) {
                temporalBin = binManager.createTemporalBin(spatialBin.getIndex());
            }
            binningContext.getBinManager().aggregateTemporalBin(spatialBin, temporalBin);
            binMap.put(temporalBin.getIndex(), temporalBin);
        }
    }
}
