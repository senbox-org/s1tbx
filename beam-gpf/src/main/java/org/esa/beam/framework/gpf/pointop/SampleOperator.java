package org.esa.beam.framework.gpf.pointop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;

/**
 * A {@code SampleOperator} may serve as a handy base class for an operator that computes a single target sample from
 * any number of source samples.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class SampleOperator extends PointOperator {

    /**
     * Computes a single target sample from the given source samples.
     * <p/>
     * The number of source samples is the maximum defined source sample index plus one. Source samples are defined
     * by using the {@link SampleConfigurer} in the
     * {@link #configureSourceSamples(SampleConfigurer) configureSourceSamples} method.
     * Attempts to read from source samples at undefined sample indices will
     * cause undefined behaviour.
     *
     * @param x             The current pixel's X coordinate.
     * @param y             The current pixel's Y coordinate.
     * @param sourceSamples The source samples (= source pixel).
     * @param targetSample  The single target sample.
     */
    protected abstract void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample);

    /**
     * Overridden to call the {@link #computeSample(int, int, Sample[], WritableSample) computeSample} method for every
     * pixel in the given tile's rectangle.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     */
    @Override
    public final void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Point location = new Point();
        final Sample[] sourceSamples = createSourceSamples(targetTile.getRectangle(), location);
        final WritableSample targetSample = createTargetSample(targetTile, location);

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
