package org.esa.beam.framework.gpf.internal;

/**
 * A simple tile cache metric which holds the average time required to process a pixel of an image.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PerformanceMetric {
    private final MeanCalc sourceNanosPerPixel;
    private final MeanCalc targetNanosPerPixel;

    public PerformanceMetric() {
        sourceNanosPerPixel = new MeanCalc();
        targetNanosPerPixel = new MeanCalc();
    }

    /**
     * @return The accumulated number of nanos per source pixel.
     */
    public double getSourceNanosPerPixel() {
        return sourceNanosPerPixel.getMean();
    }

    /**
     * @return The accumulated number of nanos per target pixel.
     */
    public double getTargetNanosPerPixel() {
        return targetNanosPerPixel.getMean();
    }

    /**
     * @return The net number of nanos per pixel (target NPP - source NPP).
     */
    public double getNanosPerPixel() {
        return getTargetNanosPerPixel() - getSourceNanosPerPixel();
    }

    public void updateSource(double value) {
        sourceNanosPerPixel.update(value);
    }

    public void updateTarget(double value) {
        targetNanosPerPixel.update(value);
    }

    private static class MeanCalc {
        private double mean;
        private long count;

        public final synchronized double getMean() {
            return mean;
        }

        public final synchronized long getCount() {
            return count;
        }

        public final synchronized void update(double value) {
            mean = (mean * count + value) / (count + 1);
            count++;
        }
    }
}
