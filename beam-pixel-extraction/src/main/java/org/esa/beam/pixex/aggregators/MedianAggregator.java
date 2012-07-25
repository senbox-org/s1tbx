package org.esa.beam.pixex.aggregators;

import java.util.Arrays;

public class MedianAggregator implements Aggregator {

    public Number[] aggregateMeasuresForBands(Number[][] allValues, int numPixels, int numBands, int[] datatypes) {
        Number[] medianValues = new Number[numBands];
        Number[][] swappedValues = swapMatrix(allValues);
        for (int i = 0; i < swappedValues.length; i++) {
            Arrays.sort(swappedValues[i]);
            medianValues[i] = swappedValues[i][swappedValues[i].length / 2];
        }
        return medianValues;
    }

    private Number[][] swapMatrix(Number[][] values) {
        Number[][] res = new Number[values[0].length][values.length];
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                res[j][i] = values[i][j];
            }
        }
        return res;
    }

}
