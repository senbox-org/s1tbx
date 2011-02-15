package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

public abstract class PixelOperator extends PointOperator {

    protected abstract void computePixel(int x, int y,
                                         Sample[] sourceSamples,
                                         WritableSample[] targetSamples);

    @Override
    public final void computeTileStack(Map<Band, Tile> targetTileStack, Rectangle targetRectangle,
                                       ProgressMonitor pm) throws OperatorException {

        final Point location = new Point();
        final DefaultSample[] sourceSamples = createSourceSamples(targetRectangle, location);
        final DefaultSample[] targetSamples = createTargetSamples(targetTileStack, location);

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
