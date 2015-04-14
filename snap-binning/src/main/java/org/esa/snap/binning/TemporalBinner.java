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

import java.io.IOException;

/**
 * Processes multiple spatial bins to a single temporal bin.
 *
 * @author Norman Fomferra
 * @see SpatialBinner
 */
public class TemporalBinner {

    private final BinManager binManager;

    public TemporalBinner(BinningContext binningContext) {
        binManager = binningContext.getBinManager();
    }

    public TemporalBin processSpatialBins(long binIndex, Iterable<? extends SpatialBin> spatialBins) throws IOException {
        return binManager.createTemporalBin(binIndex, spatialBins);
    }

    public TemporalBin computeOutput(long binIndex, TemporalBin temporalBin) {
        TemporalBin outputBin = binManager.createOutputBin(binIndex);
        binManager.computeOutput(temporalBin, outputBin.toVector());

        // will be removed soon TODO
        outputBin.setNumObs(temporalBin.getNumObs());
        outputBin.setNumPasses(temporalBin.getNumPasses());

        return outputBin;
    }
}
