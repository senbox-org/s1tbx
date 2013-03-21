package org.esa.beam.binning.operator;

import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.SpatialBinConsumer;

import java.io.IOException;

/**
 * Implementations are responsible for storing consumed {@link SpatialBin SpatialBins}.
 */
interface SpatialBinCollector extends SpatialBinConsumer {

    /**
     * Retrieves the already consumed {@link SpatialBin SpatialBins}. The bins are sorted by their bin index.
     *
     * @return A sorted map. Mapping the bin index to a list of {@link SpatialBin SpatialBins}
     *
     * @throws IOException If an IO-Exception occurs.
     */
    SpatialBinCollection getSpatialBinCollection() throws IOException;

    /**
     * Notifies this store that the consuming is completed and no more {@link SpatialBin SpatialBins} will be provided.
     *
     * @throws IOException If an IO-Exception occurs.
     */
    void consumingCompleted() throws IOException;

    /**
     * Closes all used resources.
     *
     * @throws IOException
     */
    void close() throws IOException;
}
