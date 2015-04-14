package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.DefaultRecord;
import org.junit.Test;

import static org.junit.Assert.*;

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

        assertEquals(2, minStrategy.getValueCount());
        final Number[] firstBandValues = minStrategy.getValues(defaultRecord, 0);
        final Number[] secondBandValues = minStrategy.getValues(defaultRecord, 1);

        assertEquals(2, firstBandValues.length);
        assertEquals(2, secondBandValues.length);
        assertEquals(1F, firstBandValues[0].doubleValue(), 0.0001);
        assertEquals(2F, secondBandValues[0].doubleValue(), 0.0001);
    }

}
