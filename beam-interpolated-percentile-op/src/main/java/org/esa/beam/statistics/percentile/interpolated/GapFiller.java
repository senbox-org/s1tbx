package org.esa.beam.statistics.percentile.interpolated;

public class GapFiller {

    public static void fillGaps(float[] interpolationFloats, BandConfiguration bandConfiguration) {
        fillStartAndEndWithFallback(interpolationFloats, bandConfiguration);
        float lastValue = interpolationFloats[0];
        int lastIdx = 0;
        for (int i = 1; i < interpolationFloats.length; i++) {
            float value = interpolationFloats[i];
            if (!Float.isNaN(value)) {
                final int interpolation = i - lastIdx; //
                if (interpolation > 1) {
                    final float part = (value - lastValue) / interpolation;
                    for (int j = 1; j < interpolation; j++) {
                        interpolationFloats[lastIdx + j] = lastValue + part * j;
                    }
                }
                lastValue = value;
                lastIdx = i;
            }
        }
    }

    public static void fillStartAndEndWithFallback(float[] interpolationFloats, BandConfiguration bandConfiguration) {
        if (Float.isNaN(interpolationFloats[0])) {
            interpolationFloats[0] = bandConfiguration.startValueFallback.floatValue();
        }
        final int lastIdx = interpolationFloats.length - 1;
        if (Float.isNaN(interpolationFloats[lastIdx])) {
            interpolationFloats[lastIdx] = bandConfiguration.endValueFallback.floatValue();
        }
    }

}
