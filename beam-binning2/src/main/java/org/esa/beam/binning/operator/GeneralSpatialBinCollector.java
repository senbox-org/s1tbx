package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.io.IOException;
import java.util.List;

/**
 * An implementation of {@link SpatialBinCollector} which combines a {@link MapBackedSpatialBinCollector}
 * and a {@link FileBackedSpatialBinCollector} to one spatial bin collector.
 * This means that all data is kept in memory. There are other implementations which consume less memory.
 *
 * @see MapBackedSpatialBinCollector
 * @see FileBackedSpatialBinCollector
 */
public class GeneralSpatialBinCollector implements SpatialBinCollector {

    private final FileBackedSpatialBinCollector fileBinCollector;
    private MapBackedSpatialBinCollector mapBinCollector;
    private boolean consumingCompleted;


    public GeneralSpatialBinCollector(long numBins) throws IOException {
        fileBinCollector = new FileBackedSpatialBinCollector(numBins);
        mapBinCollector = new MapBackedSpatialBinCollector();
        consumingCompleted = false;
    }

    @Override
    public void consumeSpatialBins(BinningContext ctx, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        mapBinCollector.consumeSpatialBins(ctx, spatialBins);
        if (mapBinCollector.getSpatialBinCollection().size() > 12000) {
            moveBinsToFile(ctx);
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
        mapBinCollector.close();
        mapBinCollector = null;
        fileBinCollector.consumingCompleted();
    }

    @Override
    public void close() throws IOException {
        if (fileBinCollector != null) {
            fileBinCollector.close();
        }
        if (mapBinCollector != null) {
            mapBinCollector.close();
        }
    }

    private void moveBinsToFile(BinningContext ignored) throws IOException {
        SpatialBinCollection spatialBinCollection = mapBinCollector.getSpatialBinCollection();

        Iterable<List<SpatialBin>> values = spatialBinCollection.getBinCollection();
        for (List<SpatialBin> value : values) {
            try {
                fileBinCollector.consumeSpatialBins(ignored, value);
            } catch (Exception e) {
                throw new IOException(e);
            }

        }
        mapBinCollector.clearMap();
    }
}
