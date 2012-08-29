package org.esa.beam.binning;

import org.esa.beam.binning.support.ObservationImpl;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementation of Iterator interface which iterates over {@link Observation Observations}.
 * To better support a streaming processing instances of {@link Observation} are generated on the fly each time
 * {@link org.esa.beam.binning.ObservationIterator#next() next()} is called.
 *
 * @author Marco Peters
 */
class ObservationIterator implements Iterator<Observation> {

    private Raster[] sourceTiles;
    private Raster maskTile;
    private GeoCoding gc;
    private final SamplePointer pointer;



    ObservationIterator(Raster[] sourceTiles, Raster maskTile, float[] superSamplingSteps, GeoCoding gc) {
        this.sourceTiles = sourceTiles;
        this.maskTile = maskTile;
        this.gc = gc;
        int numSuperSamplingSteps = superSamplingSteps.length * superSamplingSteps.length;
        Point2D.Float[] superSamplingPoints = new Point2D.Float[numSuperSamplingSteps];
        int index = 0;
        for (float dy : superSamplingSteps) {
            for (float dx : superSamplingSteps) {
                superSamplingPoints[index++] = new Point2D.Float(dx, dy);
            }
        }
        pointer = new SamplePointer(maskTile.getBounds(), superSamplingPoints);
    }

    @Override
    public boolean hasNext() {
        return pointer.canMove();
    }

    @Override
    public Observation next() {
        if (!hasNext()) {
            throw new NoSuchElementException("EMPTY");
        }
        pointer.move();
        int localX = pointer.getX();
        int localY = pointer.getY();
        final PixelPos pixelPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        ObservationImpl observation = null;
        if (maskTile.getSample(localX, localY, 0) != 0) {
            final float[] samples = createObservationSamples(localX, localY);
            Point2D.Float superSamplingPoint = pointer.getSuperSamplingPoint();
            pixelPos.setLocation(localX + superSamplingPoint.x, localY + superSamplingPoint.y);
            gc.getGeoPos(pixelPos, geoPos);
            observation = new ObservationImpl(geoPos.lat, geoPos.lon, samples);
        }
        return observation;
    }

    private float[] createObservationSamples(int x, int y) {
        final float[] samples = new float[sourceTiles.length];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = sourceTiles[i].getSampleFloat(x, y, 0);
        }
        return samples;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removing of elements is not allowed");
    }

    static class SamplePointer {

        private int x;
        private int y;
        private int x1;
        private int x2;
        private int y2;
        private final Point2D.Float[] superSamplingPoints;
        private int superSamplingIndex;

        SamplePointer(Rectangle bounds, Point2D.Float[] superSamplingPoints) {
            this.superSamplingPoints = superSamplingPoints;
            x1 = bounds.x;
            x2 = x1 + bounds.width;
            x = x1;
            y = bounds.y;
            y2 = bounds.y + bounds.height;
            superSamplingIndex = -1;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Point2D.Float getSuperSamplingPoint() {
            return superSamplingPoints[superSamplingIndex];
        }

        public void move() {
            if (!canMove()) {
                throw new IllegalStateException("End of Samples!");
            }
            superSamplingIndex++;
            if (superSamplingIndex == superSamplingPoints.length) {
                superSamplingIndex = 0;
                x++;
                if (x == x2) {
                    x = x1;
                    y++;
                }
            }
        }

        public boolean canMove() {
            boolean canMoveX = x < x2 - 1;
            boolean canMoveY = y < y2 - 1;
            boolean canMoveSamplePoint = superSamplingIndex < superSamplingPoints.length - 1;
            return canMoveX || canMoveY || canMoveSamplePoint;
        }
    }
}
