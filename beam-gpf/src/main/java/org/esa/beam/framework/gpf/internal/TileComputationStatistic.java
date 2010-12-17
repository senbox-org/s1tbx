package org.esa.beam.framework.gpf.internal;

import java.awt.Rectangle;

/**
 * Simple statistics for the pixel computations of a tile.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public class TileComputationStatistic {
    private final int tileX;
    private final int tileY;
    private int count;
    private double nanosSum;
    private double nanosSumSqr;
    private double nanosMin = Long.MAX_VALUE;
    private double nanosMax = Long.MIN_VALUE;

    public TileComputationStatistic(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public void tileComputed(double nanos) {
        count++;
        nanosSum += nanos;
        nanosSumSqr += nanos * nanos;
        nanosMin = Math.min(nanosMin, nanos);
        nanosMax = Math.max(nanosMax, nanos);
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public int getCount() {
        return count;
    }

    public double getNanosSum() {
        return nanosSum;
    }

    public double getNanosMean() {
        return nanosSum / count;
    }

    public double getNanosSigma() {
        double mean = nanosSum / count;
        return Math.sqrt(nanosSumSqr / count - mean * mean);
    }

    public double getNanosMin() {
        return nanosMin;
    }

    public double getNanosMax() {
        return nanosMax;
    }
}
