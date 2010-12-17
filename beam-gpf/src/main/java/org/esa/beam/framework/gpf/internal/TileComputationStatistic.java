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
    private long nanosMin = Long.MAX_VALUE;
    private long nanosMax = Long.MIN_VALUE;

    public TileComputationStatistic(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public void tileComputed(long nanos) {
        count++;
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


    public long getNanosMin() {
        return nanosMin;
    }

    public long getNanosMax() {
        return nanosMax;
    }
}
