package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

public abstract class SampleOperator extends PointOperator {

    protected abstract void computeSample(int x, int y,
                                          Sample[] sourceSamples,
                                          WritableSample targetSample);

    @Override
    public final void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final DefaultSample[] sourceSamples = createSourceSamples(targetTile.getRectangle());
        final DefaultSample targetSample = createTargetSample(targetTile);

        final int x1 = targetTile.getMinX();
        final int y1 = targetTile.getMinY();
        final int x2 = targetTile.getMaxX();
        final int y2 = targetTile.getMaxY();

        try {
            pm.beginTask(getId(), targetTile.getHeight());
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    setSampleLocations(x, y, sourceSamples);
                    setSampleLocations(x, y, targetSample);
                    computeSample(x, y, sourceSamples, targetSample);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
