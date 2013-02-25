package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.io.IOException;
import java.util.List;

/**
 * @author Marco Peters
 */
class GeneralSpatialBinCollector implements SpatialBinCollector {

    private final SpatialBinCollector fileBinCollector;
    private SpatialBinCollector memoryBinCollector;
    private boolean consumingCompleted;


    public GeneralSpatialBinCollector() throws Exception {
        fileBinCollector = new FileBackedSpatialBinCollector();
        memoryBinCollector = new MemoryBackedSpatialBinCollector();
        consumingCompleted = false;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        memoryBinCollector.consumeSpatialBins(ignored, spatialBins);
        if (memoryBinCollector.getSpatialBinCollection().size() > 12000) {
            moveBinsToFile(ignored);
        }

    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        return fileBinCollector.getSpatialBinCollection();
    }

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted = true;
        moveBinsToFile(null);
        memoryBinCollector.consumingCompleted();
        memoryBinCollector = null;
        fileBinCollector.consumingCompleted();

    }

    private void moveBinsToFile(BinningContext ignored) throws IOException {
        SpatialBinCollection spatialBinMap = memoryBinCollector.getSpatialBinCollection();

        Iterable<List<SpatialBin>> values = spatialBinMap.getCollectedBins();
        for (List<SpatialBin> value : values) {
            try {
                fileBinCollector.consumeSpatialBins(ignored, value);
            } catch (Exception e) {
                throw new IOException(e);
            }

        }
        spatialBinMap.clear();
    }
}
