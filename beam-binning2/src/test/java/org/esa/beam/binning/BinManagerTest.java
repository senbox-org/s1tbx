package org.esa.beam.binning;

import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.esa.beam.binning.cellprocessor.FeatureSelection;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinManagerTest {

    @Test
    public void testBinCreation() {
        VariableContext variableContext = createVariableContext();
        BinManager binManager = new BinManager(variableContext,
                new AggregatorAverage(variableContext, "c", null),
                new AggregatorAverageML(variableContext, "b", null),
                new AggregatorMinMax(variableContext, "a"),
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

    @Test
    public void testGetResultFeatureNames_noPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final BinManager binManager = new BinManager(variableContext,
                new AggregatorAverage(variableContext, "d", null));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(2, resultFeatureNames.length);
        assertEquals("d_mean", resultFeatureNames[0]);
        assertEquals("d_sigma", resultFeatureNames[1]);
    }

    @Test
    public void testGetResultFeatureCount_noPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final BinManager binManager = new BinManager(variableContext,
                new AggregatorAverageML(variableContext, "e", null));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(4, featureCount);
    }

    @Test
    public void testGetResultFeatureNames_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("e_min");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "e"));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(1, resultFeatureNames.length);
        assertEquals("e_min", resultFeatureNames[0]);
    }

    @Test
    public void testGetResultFeatureCount_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("f_max");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "f"));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(1, featureCount);
    }

    private VariableContext createVariableContext() {
        return new MyVariableContext("a", "b", "c");
    }
}
