package org.esa.snap.core.gpf.pointop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

/**
 * A {@code PixelOperator} may serve as a handy base class for an operator that computes any number of target samples
 * from any number of source samples.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9, revised in SNAP 2.0
 */
public abstract class PixelOperator extends PointOperator {

    /**
     * Computes the target samples from the given source samples.
     * <p>
     * The number of source/target samples is the maximum defined sample index plus one. Source/target samples are defined
     * by using the respective sample configurer in the
     * {@link #configureSourceSamples(SourceSampleConfigurer) configureSourceSamples} and
     * {@link #configureTargetSamples(TargetSampleConfigurer) configureTargetSamples} methods.
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
     * @param pm              A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target rasters
     */
    @Override
    public final void computeTileStack(Map<Band, Tile> targetTileStack, Rectangle targetRectangle,
                                       ProgressMonitor pm) throws OperatorException {

        final Point location = new Point();
        final Sample[] sourceSamples = createSourceSamples(targetRectangle, location);
        final Sample sourceMaskSamples = createSourceMaskSamples(targetRectangle, location);
        final WritableSample[] targetSamples = createTargetSamples(targetTileStack, location);

        final int x1 = targetRectangle.x;
        final int y1 = targetRectangle.y;
        final int x2 = x1 + targetRectangle.width - 1;
        final int y2 = y1 + targetRectangle.height - 1;

        try {
            pm.beginTask(getId(), targetRectangle.height);
            if (sourceMaskSamples != null) {
                for (location.y = y1; location.y <= y2; location.y++) {
                    for (location.x = x1; location.x <= x2; location.x++) {
                        if (sourceMaskSamples.getBoolean()) {
                            computePixel(location.x, location.y, sourceSamples, targetSamples);
                        } else {
                            setInvalid(targetSamples);
                        }
                    }
                    pm.worked(1);
                }

            } else {
                for (location.y = y1; location.y <= y2; location.y++) {
                    for (location.x = x1; location.x <= x2; location.x++) {
                        computePixel(location.x, location.y, sourceSamples, targetSamples);
                    }
                    pm.worked(1);
                }

            }
        } finally {
            pm.done();
        }
    }

    protected void setInvalid(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            RasterDataNode node = targetSample.getNode();
            if (node != null) {
                targetSample.set(node.getGeophysicalNoDataValue());
            }
        }
    }
}
