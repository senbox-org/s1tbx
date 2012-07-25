package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;

public class MaxAggregatorStrategy implements AggregatorStrategy {


    /*
        @Override
        public Number[] aggregateMeasuresForBands(Record allValues, int numPixels, int numBands, int[] rasterDataTypes) {
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
    */

    @Override
    public float getValue(Object attributeValue) {
        if (attributeValue instanceof AggregatedNumber) {
            return (float) ((AggregatedNumber) attributeValue).max;
        }
        throw new IllegalStateException();
    }
}
