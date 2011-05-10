package org.esa.beam.framework.gpf.pointop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

/**
 * A {@code PixelOperator} may serve as a handy base class for an operator that computes any number of target samples
 * from any number of source samples.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class PixelOperator extends PointOperator {

    /**
     * Computes the target samples from the given source samples.
     * <p/>
     * The number of source/target samples is the maximum defined sample index plus one. Source/target samples are defined
     * by using the respective {@link SampleConfigurer} in the
     * {@link #configureSourceSamples(SampleConfigurer) configureSourceSamples} and
     * {@link #configureTargetSamples(SampleConfigurer) configureTargetSamples} methods.
     * Attempts to read from source samples or write to target samples at undefined sample indices will
     * cause undefined behaviour.
     *
     * @param x             The current pixel's X coordinate.
     * @param y             The current pixel's Y coordinate.
     * @param sourceSamples The source samples (= source pixel).
     * @param targetSamples The target samples (= target pixel).
     */
    protected abstract void computePixel(int x, int y,
                                         Sample[] sourceSamples,
                                         WritableSample[] targetSamples);

    /*
     * Overridden to call the {@link #computePixel(int, int, Sample[], WritableSample[]) computePixel} method for every
     * pixel in the given target rectangle.
     *
     * @param targetTileStack The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     */
    @Override
    public final void computeTileStack(Map<Band, Tile> targetTileStack, Rectangle targetRectangle,
                                       ProgressMonitor pm) throws OperatorException {

        final Point location = new Point();
        final Sample[] sourceSamples = createSourceSamples(targetRectangle, location);
        final WritableSample[] targetSamples = createTargetSamples(targetTileStack, location);

        final int x1 = targetRectangle.x;
        final int y1 = targetRectangle.y;
        final int x2 = x1 + targetRectangle.width - 1;
        final int y2 = y1 + targetRectangle.height - 1;

        try {
            pm.beginTask(getId(), targetRectangle.height);
            for (location.y = y1; location.y <= y2; location.y++) {
                for (location.x = x1; location.x <= x2; location.x++) {
                    computePixel(location.x, location.y, sourceSamples, targetSamples);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
