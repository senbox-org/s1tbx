package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.io.IOException;
import java.util.List;

/**
 * An implementation of {@link SpatialBinCollector} which combines a {@link MapBackedSpatialBinCollector} and a {@link FileBackedSpatialBinCollector} to one spatial bin collector.
 * This means that all data is kept in memory. There are other implementations which consume less memory.
 *
 * @see MapBackedSpatialBinCollector
 * @see FileBackedSpatialBinCollector
 */
public class GeneralSpatialBinCollector implements SpatialBinCollector {

    private final SpatialBinCollector fileBinCollector;
    private SpatialBinCollector mapBinCollector;
    private boolean consumingCompleted;


    public GeneralSpatialBinCollector() throws Exception {
        fileBinCollector = new FileBackedSpatialBinCollector();
        mapBinCollector = new MapBackedSpatialBinCollector();
        consumingCompleted = false;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        mapBinCollector.consumeSpatialBins(ignored, spatialBins);
        if (mapBinCollector.getSpatialBinCollection().size() > 12000) {
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
        mapBinCollector.consumingCompleted();
        mapBinCollector = null;
        fileBinCollector.consumingCompleted();

    }

    private void moveBinsToFile(BinningContext ignored) throws IOException {
        SpatialBinCollection spatialBinMap = mapBinCollector.getSpatialBinCollection();

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
