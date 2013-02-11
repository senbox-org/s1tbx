package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Marco Peters
 */
public class GeneralSpatialBinStore implements SpatialBinStore {

    private final SpatialBinStore fileBinStore;
    private SpatialBinStore memoryBinStore;
    private boolean consumingCompleted;


    public GeneralSpatialBinStore() throws Exception {
        fileBinStore = new FileBackedSpatialBinStore();
        memoryBinStore = new MemoryBackedSpatialBinStore();
        consumingCompleted = false;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if(consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        memoryBinStore.consumeSpatialBins(ignored, spatialBins);
        if(memoryBinStore.getSpatialBinMap().size() > 12000) {
            moveBinsToFile(ignored);
        }

    }

    @Override
    public SortedSpatialBinList getSpatialBinMap() throws IOException {
        return fileBinStore.getSpatialBinMap();
    }

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted = true;
        moveBinsToFile(null);
        memoryBinStore.consumingCompleted();
        memoryBinStore = null;
        fileBinStore.consumingCompleted();

    }

    private void moveBinsToFile(BinningContext ignored) throws IOException {
        SortedSpatialBinList spatialBinMap = memoryBinStore.getSpatialBinMap();

        Iterator<List<SpatialBin>> values = spatialBinMap.values();
        while (values.hasNext()) {
            try {
                fileBinStore.consumeSpatialBins(ignored, values.next());
            } catch (Exception e) {
                throw new IOException(e);
            }

        }
        spatialBinMap.clear();
    }
}
