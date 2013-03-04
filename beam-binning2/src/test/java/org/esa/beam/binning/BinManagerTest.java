package org.esa.beam.binning;

import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinManagerTest {

    @Test
    public void testBinCreation() {
        VariableContext variableContext = new MyVariableContext("a", "b", "c");
        BinManager binManager = new BinManager(variableContext,
                                               new AggregatorAverage(variableContext, "c", null, null),
                                               new AggregatorAverageML(variableContext, "b", null, null),
                                               new AggregatorMinMax(variableContext, "a", null),
                                               new AggregatorOnMaxSet(variableContext, "c", "a", "b"));

        assertEquals(4, binManager.getAggregatorCount());

        SpatialBin sbin = binManager.createSpatialBin(42);
        assertEquals(42, sbin.getIndex());
        assertEquals(2 + 2 + 2 + 4, sbin.getFeatureValues().length);

        TemporalBin tbin = binManager.createTemporalBin(42);
        assertEquals(42, tbin.getIndex());
        assertEquals(3 + 3 + 2 + 4, tbin.getFeatureValues().length);
    }

    @Test
    public void testNameUnifying() throws Exception {
        BinManager.NameUnifier nameUnifier = new BinManager.NameUnifier();
        assertEquals("expression_p90", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p90_1", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p90_2", nameUnifier.unifyName("expression_p90"));
        assertEquals("expression_p50", nameUnifier.unifyName("expression_p50"));
        assertEquals("expression_p50_1", nameUnifier.unifyName("expression_p50"));


    }
}
