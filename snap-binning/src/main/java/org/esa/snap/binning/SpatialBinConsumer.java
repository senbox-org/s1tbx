/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.binning;

import java.util.List;

/**
 * Consumes a slice of spatial bins that are emitted by the {@link SpatialBinner}.
 *
 * @author Norman Fomferra
 */
public interface SpatialBinConsumer {
    /**
     * Consumes an unsorted list of spatial bins.
     *
     * @param binningContext The binning context.
     * @param spatialBins    An unsorted list of spatial bins.
     * @throws Exception If any error occurs.
     */
    void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) throws Exception;
}
