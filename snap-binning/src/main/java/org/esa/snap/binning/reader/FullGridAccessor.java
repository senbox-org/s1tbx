package org.esa.snap.binning.reader;

import org.esa.snap.core.datamodel.Band;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

class FullGridAccessor extends AbstractGridAccessor {

    final private NcArrayCache ncArrayCache;
    private final NetcdfFile netcdfFile;

    FullGridAccessor(NetcdfFile netcdfFile) {
        this.netcdfFile = netcdfFile;

        ncArrayCache = new NcArrayCache();
    }

    @Override
    Array getLineValues(Band destBand, VariableReader variableReader, int lineIndex) throws IOException {
        Array lineValues;

        synchronized (ncArrayCache) {
            CacheEntry cacheEntry = ncArrayCache.get(destBand);
            if (cacheEntry != null) {
                lineValues = cacheEntry.getData();
            } else {
                synchronized (netcdfFile) {
                    lineValues = variableReader.readFully();
                }
                ncArrayCache.put(destBand, new CacheEntry(lineValues));
            }
        }

        return lineValues;
    }

    @Override
    int getStartBinIndex(int sourceOffsetX, int lineIndex) {
        return getBinIndexInPlanetaryGrid(sourceOffsetX, lineIndex);
    }

    @Override
    int getEndBinIndex(int sourceOffsetX, int sourceWidth, int lineIndex) {
        return getBinIndexInPlanetaryGrid(sourceOffsetX + sourceWidth - 1, lineIndex) + 1;
    }

    @Override
    int getBinIndexInGrid(int binIndex, int lineIndex) {
        return binIndex;
    }

    @Override
    void dispose() {
        ncArrayCache.dispose();
    }

    private int getBinIndexInPlanetaryGrid(int x, int y) {
        final int numberOfBinsInRow = planetaryGrid.getNumCols(y);
        final double longitudeExtentPerBin = 360.0 / numberOfBinsInRow;
        final double pixelCenterLongitude = x * pixelSizeX + pixelSizeX / 2;
        final int firstBinIndex = (int) planetaryGrid.getFirstBinIndex(y);
        return ((int) (pixelCenterLongitude / longitudeExtentPerBin)) + firstBinIndex;
    }
}
