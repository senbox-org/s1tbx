package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.DefaultRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

        assertEquals(2, meanStrategy.getValueCount());
        final float[] firstBandValues = meanStrategy.getValues(defaultRecord, 0);
        final float[] secondBandValues = meanStrategy.getValues(defaultRecord, 1);

        assertEquals(2, firstBandValues.length);
        assertEquals(2, secondBandValues.length);

        assertEquals(3.5F, firstBandValues[0], 0.0001);
        assertEquals(1.8708F, firstBandValues[1], 0.0001);
        assertEquals(4.5, secondBandValues[0], 0.0001);
        assertEquals(1.8708F, secondBandValues[1], 0.0001);
    }
}
