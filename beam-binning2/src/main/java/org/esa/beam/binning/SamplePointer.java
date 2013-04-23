package org.esa.beam.binning;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;

/**
 * @author Marco Peters
 */
abstract class SamplePointer {

    public static SamplePointer create(Raster[] sourceTiles, Rectangle bounds) {
        return new SamplePointerNoSuperSampling(sourceTiles, bounds);
    }

    public static SamplePointer create(Raster[] sourceTiles, Rectangle bounds, Point2D.Float[] superSamplingPoints) {
        return new SamplePointerImpl(sourceTiles, bounds, superSamplingPoints);
    }

    public static Point2D.Float[] createSamplingPoints(float[] samplingSteps) {
        int numSuperSamplingSteps = samplingSteps.length * samplingSteps.length;
        Point2D.Float[] superSamplingPoints = new Point2D.Float[numSuperSamplingSteps];
        int index = 0;
        for (float dy : samplingSteps) {
            for (float dx : samplingSteps) {
                superSamplingPoints[index++] = new Point2D.Float(dx, dy);
            }
        }
        return superSamplingPoints;
    }

    abstract void move();

    abstract boolean canMove();

    abstract int getX();

    abstract int getY();

    abstract Point2D.Float getSuperSamplingPoint();

    abstract float[] createSamples();


    private static final class SamplePointerImpl extends SamplePointer {

        private int x;
        private int y;
        private int x1;
        private int x2;
        private int y2;
        private Raster[] sourceTiles;
        private final Point2D.Float[] superSamplingPoints;
        private int superSamplingIndex;
        private int lastX;
        private int lastY;
        private float[] lastSamples;


        SamplePointerImpl(Raster[] sourceTiles, Rectangle bounds, Point2D.Float[] superSamplingPoints) {
            this.sourceTiles = sourceTiles;
            this.superSamplingPoints = superSamplingPoints;
            x1 = bounds.x;
            x2 = x1 + bounds.width;
            x = x1;
            y = bounds.y;
            y2 = bounds.y + bounds.height;
            superSamplingIndex = -1;
            lastX = x - 1;
            lastY = y - 1;
            lastSamples = new float[sourceTiles.length];
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public Point2D.Float getSuperSamplingPoint() {
            return superSamplingPoints[superSamplingIndex];
        }

        @Override
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

        @Override
        public boolean canMove() {
            boolean canMoveX = x < x2 - 1;
            boolean canMoveY = y < y2 - 1;
            boolean canMoveSamplePoint = superSamplingIndex < superSamplingPoints.length - 1;
            return canMoveX || canMoveY || canMoveSamplePoint;
        }

        @Override
        public float[] createSamples() {
            if (hasSampleLocationChanged(x, y)) {
                lastX = x;
                lastY = y;
                for (int i = 0; i < lastSamples.length; i++) {
                    lastSamples[i] = sourceTiles[i].getSampleFloat(x, y, 0);
                }
            }
            return lastSamples;
        }

        private boolean hasSampleLocationChanged(int x, int y) {
            return x != lastX || y != lastY;
        }

    }

    private static final class SamplePointerNoSuperSampling extends SamplePointer {

        private static final Point2D.Float CENTER = new Point2D.Float(0.5f, 0.5f);

        private int x;
        private int y;
        private int x1;
        private int x2;
        private int y2;
        private Raster[] sourceTiles;


        SamplePointerNoSuperSampling(Raster[] sourceTiles, Rectangle bounds) {
            this.sourceTiles = sourceTiles;
            x1 = bounds.x;
            x2 = x1 + bounds.width;
            x = x1 - 1;
            y = bounds.y;
            y2 = bounds.y + bounds.height;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public Point2D.Float getSuperSamplingPoint() {
            return CENTER;
        }

        @Override
        public void move() {
            if (!canMove()) {
                throw new IllegalStateException("End of Samples!");
            }
            x++;
            if (x == x2) {
                x = x1;
                y++;
            }
        }

        @Override
        float[] createSamples() {
            float[] samples = new float[sourceTiles.length];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = sourceTiles[i].getSampleFloat(x, y, 0);
            }
            return samples;
        }

        @Override
        public boolean canMove() {
            boolean canMoveX = x < x2 - 1;
            boolean canMoveY = y < y2 - 1;
            return canMoveX || canMoveY;
        }
    }
}
