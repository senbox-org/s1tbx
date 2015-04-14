/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.cellprocessor.FeatureSelection;
import org.esa.snap.binning.support.ObservationImpl;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class CellProcessorTest {

    @Test
    public void testBinningWithoutPostProcessor() throws IOException {
        MyVariableContext variableContext = new MyVariableContext("A");
        BinManager bman = new BinManager(variableContext, new AggregatorMinMax(variableContext, "A", "out"));

        TemporalBin tbin = doBinning(bman);
        assertEquals(2, tbin.getFeatureValues().length);

        Vector resultVector = tbin.toVector();
        assertEquals(2, resultVector.size());
        assertEquals(0.2f, resultVector.get(0), 1e-4);
        assertEquals(0.6f, resultVector.get(1), 1e-4);

        assertArrayEquals(new String[]{"out_min", "out_max"}, bman.getResultFeatureNames());
    }

    @Test
    public void testBinningWithPostProcessor() throws IOException {
        MyVariableContext variableContext = new MyVariableContext("A");
        AggregatorMinMax aggregator = new AggregatorMinMax(variableContext, "A", "out");
        FeatureSelection.Config ppSelection = new FeatureSelection.Config("out_max");
        BinManager bman = new BinManager(variableContext, ppSelection, aggregator);

        TemporalBin tbin = doBinning(bman);
        assertEquals(2, tbin.getFeatureValues().length);

        final Vector temporalVector = tbin.toVector();
        int postProcessFeatureCount = bman.getPostProcessFeatureCount();
        TemporalBin processedBin = new TemporalBin(tbin.getIndex(), postProcessFeatureCount);

        WritableVector processedVector = processedBin.toVector();

        bman.postProcess(temporalVector, processedVector);

        Vector resultVector = processedBin.toVector();
        assertEquals(1, resultVector.size());
        assertEquals(0.6f, resultVector.get(0), 1e-4);

        assertArrayEquals(new String[]{"out_max"}, bman.getResultFeatureNames());
    }

    private TemporalBin doBinning(BinManager bman) {
        SpatialBin sbin;
        TemporalBin tbin;

        tbin = bman.createTemporalBin(0);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.2f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.6f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.4f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        bman.completeTemporalBin(tbin);

        assertEquals(3, tbin.getNumObs());

        Vector tVec = bman.getTemporalVector(tbin, 0);

        assertEquals(2, tVec.size());
        assertEquals(0.2f, tVec.get(0), 1e-5f);
        assertEquals(0.6f, tVec.get(1), 1e-5f);

        return tbin;
    }
}
