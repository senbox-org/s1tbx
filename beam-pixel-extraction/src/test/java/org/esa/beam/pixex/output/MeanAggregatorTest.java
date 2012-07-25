package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.pixex.aggregators.MeanAggregator;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MeanAggregatorTest {

    @Test
    public void testMeanAggregator() throws Exception {
        //preparation
        MeanAggregator aggregator = new MeanAggregator();
        final int numPixels = 9;
        final int numBands = 2;
        int[] dataTypes = new int[numBands];
        dataTypes[0] = ProductData.TYPE_INT32;
        dataTypes[1] = ProductData.TYPE_FLOAT32;

        //execution
        final Number[][] allValues = createNumberMatrix(numPixels, numBands);
        final Number[] aggregatedMeasures = aggregator.aggregateMeasuresForBands(allValues, numPixels, numBands,
                                                                                 dataTypes);

        //verification
        assertEquals(2, aggregatedMeasures.length);
        assertEquals(285 / 9, aggregatedMeasures[0]);
        assertEquals(375f / 9, aggregatedMeasures[1]);
    }

    private Number[][] createNumberMatrix(int numPixels, int numBands) {
        Number[][] allValues = new Number[numPixels][numBands];
        for (int i = 0; i < numPixels; i++) {
            int newInteger = (int) Math.pow(i + 1, 2);
            double newDouble = Math.pow(i + 1, 2) + 10.0;
            allValues[i] = new Number[]{newInteger, newDouble};
        }
        return allValues;
    }

}
