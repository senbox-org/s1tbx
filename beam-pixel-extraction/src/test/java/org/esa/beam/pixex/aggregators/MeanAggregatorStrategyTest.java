package org.esa.beam.pixex.aggregators;

import org.esa.beam.pixex.calvalus.ma.AggregatedNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MeanAggregatorStrategyTest {

    @Test
    public void testMeanAggregatorStrategyWithAggregatedNumber() throws Exception {
        //preparation
        MeanAggregatorStrategy aggregator = new MeanAggregatorStrategy();
        final AggregatedNumber aggregatedNumber = new AggregatedNumber(-1, -1, -1, -1, -1, 14.0, 3);

        //execution & verification
        assertEquals(2, aggregator.getValues(aggregatedNumber).length);
        assertEquals(14.0, aggregator.getValues(aggregatedNumber)[0], 0.00001);
        assertEquals(3.0, aggregator.getValues(aggregatedNumber)[1], 0.00001);
    }

}
