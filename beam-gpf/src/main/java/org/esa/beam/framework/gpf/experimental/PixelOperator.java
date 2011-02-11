package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.util.Map;

public abstract class PixelOperator extends PointOperator {

    protected abstract void computePixel(int x, int y,
                                         Sample[] sourceSamples,
                                         WritableSample[] targetSamples);

    @Override
    public final void computeTileStack(Map<Band, Tile> targetTileStack, Rectangle targetRectangle,
                                       ProgressMonitor pm) throws OperatorException {

        final DefaultSample[] sourceSamples = createSourceSamples(targetRectangle);
        final DefaultSample[] targetSamples = createTargetSamples(targetTileStack);

        final int x1 = targetRectangle.x;
        final int y1 = targetRectangle.y;
        final int x2 = x1 + targetRectangle.width - 1;
        final int y2 = y1 + targetRectangle.height - 1;

        try {
            pm.beginTask(getId(), targetRectangle.height);
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (final DefaultSample sourceSample : sourceSamples) {
                        sourceSample.setPixel(x, y);
                    }
                    for (final DefaultSample targetSample : targetSamples) {
                        targetSample.setPixel(x, y);
                    }
                    computePixel(x, y, sourceSamples, targetSamples);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
