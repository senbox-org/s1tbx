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

import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.SpatialBinConsumer;

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
