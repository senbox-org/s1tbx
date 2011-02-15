package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;

public abstract class SampleOperator extends PointOperator {

    protected abstract void computeSample(int x, int y,
                                          Sample[] sourceSamples,
                                          WritableSample targetSample);

    @Override
    public final void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Point location = new Point();
        final DefaultSample[] sourceSamples = createSourceSamples(targetTile.getRectangle(), location);
        final DefaultSample targetSample = createTargetSample(targetTile, location);

        final int x1 = targetTile.getMinX();
        final int y1 = targetTile.getMinY();
        final int x2 = targetTile.getMaxX();
        final int y2 = targetTile.getMaxY();

        try {
            pm.beginTask(getId(), targetTile.getHeight());
            for (location.y = y1; location.y <= y2; location.y++) {
                for (location.x = x1; location.x <= x2; location.x++) {
                    computeSample(location.x, location.y, sourceSamples, targetSample);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
