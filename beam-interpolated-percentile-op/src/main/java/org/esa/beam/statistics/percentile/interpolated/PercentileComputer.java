package org.esa.beam.statistics.percentile.interpolated;

public class PercentileComputer {
    public static float compute(int percentile, float[] values) {
        int pIndex = (int) Math.floor(percentile / 100f * values.length);
        return values[pIndex];
    }
}
