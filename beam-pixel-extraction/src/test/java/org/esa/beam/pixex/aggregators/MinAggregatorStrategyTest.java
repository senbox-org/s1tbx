package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.DefaultRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinAggregatorStrategyTest {

    @Test
    public void testGetValueForAggregatedNumber() throws Exception {
        AggregatorStrategy minStrategy = new MinAggregatorStrategy();

        final Float[] valuesForBand1 = {
                1F, 2F, 3F, 4F, 5F, 6F
        };

        final Integer[] valuesForBand2 = {
                2, 3, 4, 5, 6, 7
        };

        final Number[][] numbers = new Number[2][];
        numbers[0] = valuesForBand1;
        numbers[1] = valuesForBand2;

        final DefaultRecord defaultRecord = new DefaultRecord(numbers);

        assertEquals(1, minStrategy.getValueCount());
        final float[] firstBandValues = minStrategy.getValues(defaultRecord, 0);
        final float[] secondBandValues = minStrategy.getValues(defaultRecord, 1);

        assertEquals(1, firstBandValues.length);
        assertEquals(1, secondBandValues.length);
        assertEquals(1F, firstBandValues[0], 0.0001);
        assertEquals(2F, secondBandValues[0], 0.0001);
    }

}
