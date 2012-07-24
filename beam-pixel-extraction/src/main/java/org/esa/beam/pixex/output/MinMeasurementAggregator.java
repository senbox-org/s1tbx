package org.esa.beam.pixex.output;

import java.util.Arrays;

public class MinMeasurementAggregator implements MeasurementAggregator {

    @Override
    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] dataTypes) {
        Number[] meanMeasurementValues = new Number[numBands];
        Arrays.fill(meanMeasurementValues, Double.POSITIVE_INFINITY);
        for (int i = 0; i < allValues.length; i++) {
            for (int j = 0; j < allValues[j].length; j++) {
                meanMeasurementValues[j] = min(meanMeasurementValues[j], allValues[i][j]);
            }
        }
        return meanMeasurementValues;
    }

    public Number min(Number first, Number second) {
        if (first.doubleValue() < second.doubleValue()) {
            return first;
        }
        return second;
    }

}
