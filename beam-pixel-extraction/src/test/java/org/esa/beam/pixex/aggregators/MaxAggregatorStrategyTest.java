package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaxAggregatorStrategyTest {

    @Test
    public void testGetValueForAggregatedNumber() throws Exception {
        AggregatedNumber aggregatedNumber = new AggregatedNumber(-1, -1, -1, -1, 24, -1, -1);
        AggregatorStrategy maxStrategy = new MaxAggregatorStrategy();

        assertEquals(1, maxStrategy.getValueCount());
        assertEquals(24, maxStrategy.getValues(aggregatedNumber)[0], 0.0001);
    }
}
