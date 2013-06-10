package org.esa.beam.binning.cellprocessor;

import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.WritableVector;

public class CellProcessorChain {

    private final BinManager binManager;

    public CellProcessorChain(BinningContext binningContext) {
        binManager = binningContext.getBinManager();
    }

    public TemporalBin process(TemporalBin temporalBin) {
        if (binManager.hasPostProcessor()) {
            WritableVector temporalVector = temporalBin.toVector();
            TemporalBin processBin = binManager.createProcessBin(temporalBin.getIndex());
            binManager.postProcess(temporalVector, processBin.toVector());

            // will be removed soon TODO
            processBin.setNumObs(temporalBin.getNumObs());
            processBin.setNumPasses(temporalBin.getNumPasses());

            return processBin;
        } else {
            return temporalBin;
        }
    }
}
