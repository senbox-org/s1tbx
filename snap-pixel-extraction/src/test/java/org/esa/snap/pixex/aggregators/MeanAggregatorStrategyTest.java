package org.esa.snap.pixex.aggregators;

import org.esa.snap.pixex.calvalus.ma.DefaultRecord;
import org.junit.Test;

import static org.junit.Assert.*;

public class MeanAggregatorStrategyTest {

    @Test
    public void testGetValueForAggregatedNumber() throws Exception {
        AggregatorStrategy meanStrategy = new MeanAggregatorStrategy();

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

        assertEquals(3, meanStrategy.getValueCount());
        final Number[] firstBandValues = meanStrategy.getValues(defaultRecord, 0);
        final Number[] secondBandValues = meanStrategy.getValues(defaultRecord, 1);

        assertEquals(3, firstBandValues.length);
        assertEquals(3, secondBandValues.length);

        assertEquals(3.5F, firstBandValues[0].doubleValue(), 0.0001);
        assertEquals(1.8708F, firstBandValues[1].doubleValue(), 0.0001);
        assertEquals(4.5, secondBandValues[0].doubleValue(), 0.0001);
        assertEquals(1.8708F, secondBandValues[1].doubleValue(), 0.0001);
    }
}
