package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinAggregatorStrategyTest {

    @Test
    public void testGetValueForAggregatedNumber() throws Exception {
        AggregatedNumber aggregatedNumber = new AggregatedNumber(-1, -1, -1, 24, -1, -1, -1);
        AggregatorStrategy minStrategy = new MinAggregatorStrategy();

        assertEquals(1, minStrategy.getValueCount());
        assertEquals(24, minStrategy.getValues(aggregatedNumber)[0], 0.0001);
    }

}
