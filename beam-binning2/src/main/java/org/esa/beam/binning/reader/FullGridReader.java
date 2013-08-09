package org.esa.beam.binning.reader;

import org.esa.beam.framework.datamodel.Band;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

class FullGridReader extends BinnedReaderImpl {

    final private NcArrayCache ncArrayCache;
    private final NetcdfFile netcdfFile;

    FullGridReader(NetcdfFile netcdfFile) {
        this.netcdfFile = netcdfFile;

        ncArrayCache = new NcArrayCache();
    }

    @Override
    Array getLineValues(Band destBand, Variable binVariable,int lineIndex) throws IOException {
        Array lineValues;

        synchronized (ncArrayCache) {
            CacheEntry cacheEntry = ncArrayCache.get(destBand);
            if (cacheEntry != null) {
                lineValues = cacheEntry.getData();
            } else {
                synchronized (netcdfFile) {
                    lineValues = binVariable.read();
                }
                ncArrayCache.put(destBand, new CacheEntry(lineValues));
            }
        }

        return lineValues;
    }

    @Override
    int getStartBinIndex() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    int getEndBinIndex(int lineIndex) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    int getBinIndexInGrid(int binIndex, int lineIndex) {
        return binIndex;
    }

    @Override
    void dispose() {
        ncArrayCache.dispose();
    }
}
