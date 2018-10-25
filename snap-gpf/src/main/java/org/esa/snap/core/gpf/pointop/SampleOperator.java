package org.esa.snap.core.gpf.pointop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * A {@code SampleOperator} may serve as a handy base class for an operator that computes a single target sample from
 * any number of source samples.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9, revised in SNAP 2.0
 */
public abstract class SampleOperator extends PointOperator {

    /**
     * Computes a single target sample from the given source samples.
     * <p>
     * The number of source samples is the maximum defined source sample index plus one. Source samples are defined
     * by using the sample configurer in the
     * {@link #configureSourceSamples(SourceSampleConfigurer) configureSourceSamples} method.
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
     * @param pm         A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target raster
     */
    @Override
    public final void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetRectangle = targetTile.getRectangle();
        final Point location = new Point();
        final Sample[] sourceSamples = createSourceSamples(targetRectangle, location);
        final Sample sourceMaskSamples = createSourceMaskSamples(targetRectangle, location);
        final WritableSample targetSample = createTargetSample(targetTile, location);

        final int x1 = targetTile.getMinX();
        final int y1 = targetTile.getMinY();
        final int x2 = targetTile.getMaxX();
        final int y2 = targetTile.getMaxY();

        try {
            pm.beginTask(getId(), targetTile.getHeight());
            if (sourceMaskSamples != null) {
                checkForCancellation();
                for (location.y = y1; location.y <= y2; location.y++) {
                    for (location.x = x1; location.x <= x2; location.x++) {
                        if (sourceMaskSamples.getBoolean()) {
                            computeSample(location.x, location.y, sourceSamples, targetSample);
                        }else{
                            setInvalid(targetSample);
                        }
                    }
                    pm.worked(1);
                }
            } else {
                for (location.y = y1; location.y <= y2; location.y++) {
                    for (location.x = x1; location.x <= x2; location.x++) {
                        computeSample(location.x, location.y, sourceSamples, targetSample);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void setInvalid(WritableSample targetSample) {
        targetSample.set(targetSample.getNode().getGeophysicalNoDataValue());
    }
}
