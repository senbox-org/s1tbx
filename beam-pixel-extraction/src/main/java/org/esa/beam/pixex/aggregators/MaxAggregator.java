package org.esa.beam.pixex.aggregators;

import java.util.Arrays;

public class MaxAggregator implements Aggregator {

    @Override
    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] rasterDataTypes) {
        Number[] meanMeasurementValues = new Number[numBands];
        Arrays.fill(meanMeasurementValues, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < allValues.length; i++) {
            for (int j = 0; j < allValues[j].length; j++) {
                meanMeasurementValues[j] = max(meanMeasurementValues[j], allValues[i][j]);
            }
        }
        return meanMeasurementValues;
    }

    public Number max(Number first, Number second) {
        if(first.doubleValue() > second.doubleValue()) {
            return first;
        }
        return second;
    }

}
