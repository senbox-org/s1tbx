/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.cellprocessor;

import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.binning.support.BinTracer;

public class CellProcessorChain {

    private final BinManager binManager;
    private final BinTracer binTracer;

    public CellProcessorChain(BinningContext binningContext) {
        binManager = binningContext.getBinManager();
        binTracer = binManager.getBinTracer();
    }

    public TemporalBin process(TemporalBin temporalBin) {
        if (binManager.hasPostProcessor()) {
            WritableVector temporalVector = temporalBin.toVector();
            TemporalBin processBin = binManager.createProcessBin(temporalBin.getIndex());
            binManager.postProcess(temporalVector, processBin.toVector());

            // will be removed soon TODO
            processBin.setNumObs(temporalBin.getNumObs());
            processBin.setNumPasses(temporalBin.getNumPasses());

            long binIndex = temporalBin.getIndex();
            if (BinTracer.traceThis(binTracer, binIndex)) {
                binTracer.tracePost(temporalBin, processBin);
            }

            return processBin;
        } else {
            return temporalBin;
        }
    }
}
