package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.DefaultRecord;
import org.junit.Test;

import static org.junit.Assert.*;

public class MaxAggregatorStrategyTest {

    @Test
    public void testGetValueForAggregatedNumber() throws Exception {
        AggregatorStrategy maxStrategy = new MaxAggregatorStrategy();

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

        assertEquals(2, maxStrategy.getValueCount());
        final Number[] firstBandValues = maxStrategy.getValues(defaultRecord, 0);
        final Number[] secondBandValues = maxStrategy.getValues(defaultRecord, 1);

        assertEquals(2, firstBandValues.length);
        assertEquals(2, secondBandValues.length);
        assertEquals(6F, firstBandValues[0].doubleValue(), 0.0001);
        assertEquals(7F, secondBandValues[0].doubleValue(), 0.0001);
        assertEquals(6, firstBandValues[1].intValue());
        assertEquals(6, secondBandValues[1].intValue());
    }
}
