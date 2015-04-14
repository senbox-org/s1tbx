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
import org.esa.snap.binning.support.ObservationImpl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class TemporalBinTest {

    @Test
    public void testIllegalConstructorCalls() {
        try {
            new TemporalBin(0, -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testLegalConstructorCalls() {
        TemporalBin bin = new TemporalBin(42, 0);
        assertEquals(42, bin.getIndex());
        bin = new TemporalBin(43, 3);
        assertEquals(43, bin.getIndex());
    }

    @Test
    public void testBinAggregationAndIO() throws IOException {
        MyVariableContext variableContext = new MyVariableContext("A", "B", "C");
        BinManager bman = new BinManager(variableContext,
                                         new AggregatorMinMax(variableContext, "A", "out"),
                                         new AggregatorAverage(variableContext, "B", 0.0),
                                         new AggregatorAverageML(variableContext, "C", 0.5));

        SpatialBin sbin;
        TemporalBin tbin;

        tbin = bman.createTemporalBin(0);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.2f, 4.0f, 4.0f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.6f, 2.0f, 2.0f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        sbin = bman.createSpatialBin(0);
        bman.aggregateSpatialBin(new ObservationImpl(0.0, 0.0, 0.0, 0.4f, 6.0f, 6.0f), sbin);
        bman.completeSpatialBin(sbin);
        bman.aggregateTemporalBin(sbin, tbin);

        assertEquals(3, tbin.getNumObs());

        Vector agg1 = bman.getTemporalVector(tbin, 0);
        Vector agg2 = bman.getTemporalVector(tbin, 1);
        Vector agg3 = bman.getTemporalVector(tbin, 2);

        assertEquals(2, agg1.size());
        assertEquals(0.2f, agg1.get(0), 1e-5f);
        assertEquals(0.6f, agg1.get(1), 1e-5f);

        assertEquals(3, agg2.size());
        assertEquals(12.0f, agg2.get(0), 1e-5f);
        assertEquals(56.0f, agg2.get(1), 1e-5f);
        assertEquals(3.0f, agg2.get(2), 1e-5f);

        assertEquals(3, agg3.size());
        assertEquals(3.871201f, agg3.get(0), 1e-5f);
        assertEquals(5.612667f, agg3.get(1), 1e-5f);
        assertEquals(3.0f, agg3.get(2), 1e-5f);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tbin.write(new DataOutputStream(baos));
        byte[] bytes = baos.toByteArray();

        TemporalBin tbinCopy = TemporalBin.read(new DataInputStream(new ByteArrayInputStream(bytes)));

        assertEquals(-1, tbinCopy.getIndex());
        assertEquals(3, tbinCopy.getNumObs());

        Vector agg1Copy = bman.getTemporalVector(tbinCopy, 0);
        Vector agg2Copy = bman.getTemporalVector(tbinCopy, 1);
        Vector agg3Copy = bman.getTemporalVector(tbinCopy, 2);

        assertEquals(2, agg1.size());
        assertEquals(0.2f, agg1Copy.get(0), 1e-5f);
        assertEquals(0.6f, agg1Copy.get(1), 1e-5f);

        assertEquals(3, agg2.size());
        assertEquals(12.0f, agg2Copy.get(0), 1e-5f);
        assertEquals(56.0f, agg2Copy.get(1), 1e-5f);
        assertEquals(3.0f, agg2Copy.get(2), 1e-5f);

        assertEquals(3, agg3.size());
        assertEquals(3.871201f, agg3Copy.get(0), 1e-5f);
        assertEquals(5.612667f, agg3Copy.get(1), 1e-5f);
        assertEquals(3.0f, agg3Copy.get(2), 1e-5f);
    }

    @Test
    public void testToString() {
        TemporalBin bin = new TemporalBin(42, 0);
        assertEquals("TemporalBin{index=42, numObs=0, numPasses=0, featureValues=[]}", bin.toString());
        bin = new TemporalBin(43, 3);
        assertEquals("TemporalBin{index=43, numObs=0, numPasses=0, featureValues=[0.0, 0.0, 0.0]}", bin.toString());
        bin.setNumPasses(7);
        bin.setNumObs(3);
        assertEquals("TemporalBin{index=43, numObs=3, numPasses=7, featureValues=[0.0, 0.0, 0.0]}", bin.toString());
        bin.featureValues[0] = 1.2f;
        bin.featureValues[2] = 2.4f;
        assertEquals("TemporalBin{index=43, numObs=3, numPasses=7, featureValues=[1.2, 0.0, 2.4]}", bin.toString());
    }


    @Test
    public void testBinCreationWithIndex() throws Exception {
        final TemporalBin bin = TemporalBin.read(10L, new DataInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        }));
        assertEquals(10L, bin.getIndex());
    }
}
