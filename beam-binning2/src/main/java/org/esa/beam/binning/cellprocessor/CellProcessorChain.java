package org.esa.beam.binning.cellprocessor;

import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.binning.support.VectorImpl;

public class CellProcessorChain {

    private final BinManager binManager;

    public CellProcessorChain(BinningContext binningContext) {
        binManager = binningContext.getBinManager();
    }

    public TemporalBin process(TemporalBin temporalBin) {
        if (binManager.hasPostProcessor()) {
            WritableVector temporalVector = new VectorImpl(temporalBin.getFeatureValues());

            int postProcessFeatureCount = binManager.getPostProcessFeatureCount();
            TemporalBin processedBin = new TemporalBin(temporalBin.getIndex(), postProcessFeatureCount);
            WritableVector processedVector = new VectorImpl(processedBin.getFeatureValues());

            binManager.postProcess(temporalVector, processedVector);

            return processedBin;
        } else {
            return temporalBin;
        }
    }
}
