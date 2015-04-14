/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.binning;

import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.aggregators.AggregatorAverageML;
import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.aggregators.AggregatorOnMaxSet;
import org.esa.snap.binning.cellprocessor.FeatureSelection;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinManagerTest {

    @Test
    public void testBinCreation() {
        VariableContext variableContext = createVariableContext();
        BinManager binManager = new BinManager(variableContext,
                new AggregatorAverage(variableContext, "c", 0.0),
                new AggregatorAverageML(variableContext, "b", 0.5),
                new AggregatorMinMax(variableContext, "a", "a"),
                new AggregatorOnMaxSet(variableContext, "c", "c", "a", "b"));

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
                new AggregatorAverage(variableContext, "d", 0.0));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(2, resultFeatureNames.length);
        assertEquals("d_mean", resultFeatureNames[0]);
        assertEquals("d_sigma", resultFeatureNames[1]);
    }

    @Test
    public void testGetResultFeatureCount_noPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final BinManager binManager = new BinManager(variableContext,
                new AggregatorAverageML(variableContext, "e", 0.5));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(4, featureCount);
    }

    @Test
    public void testGetResultFeatureNames_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("out_min");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "e", "out"));

        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        assertEquals(1, resultFeatureNames.length);
        assertEquals("out_min", resultFeatureNames[0]);
    }

    @Test
    public void testGetResultFeatureCount_withPostProcessor_targetName() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("out_max");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "f", "out"));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(1, featureCount);
    }

    @Test
    public void testGetResultFeatureCount_withPostProcessor() {
        final VariableContext variableContext = createVariableContext();
        final FeatureSelection.Config ppSelection = new FeatureSelection.Config("f_max");

        final BinManager binManager = new BinManager(variableContext,
                ppSelection,
                new AggregatorMinMax(variableContext, "f", "f"));

        final int featureCount = binManager.getResultFeatureCount();
        assertEquals(1, featureCount);
    }

    private VariableContext createVariableContext() {
        return new MyVariableContext("a", "b", "c");
    }
}
