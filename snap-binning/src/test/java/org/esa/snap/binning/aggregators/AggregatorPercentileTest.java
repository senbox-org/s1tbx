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

package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.MyVariableContext;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.*;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class AggregatorPercentileTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata_P90() {
        AggregatorPercentile agg = new AggregatorPercentile(new MyVariableContext("c"), "c", "target", 90);

        assertEquals("c_sum", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_p90", agg.getTemporalFeatureNames()[0]);
        assertEquals("target_p90", agg.getOutputFeatureNames()[0]);
    }

    @Test
    public void testMetadata_P70() {
        AggregatorPercentile agg = new AggregatorPercentile(new MyVariableContext("c"), "c", "c", 70);

        assertEquals("PERCENTILE", agg.getName());

        assertEquals(1, agg.getSpatialFeatureNames().length);
        assertEquals("c_sum", agg.getSpatialFeatureNames()[0]);

        assertEquals(1, agg.getTemporalFeatureNames().length);
        assertEquals("c_p70", agg.getTemporalFeatureNames()[0]);

        assertEquals(1, agg.getOutputFeatureNames().length);
        assertEquals("c_p70", agg.getOutputFeatureNames()[0]);

    }

    @Test
    public void testAggregatorPercentile() {
        AggregatorPercentile agg = new AggregatorPercentile(new MyVariableContext("c"), "c", "c", 70);

        VectorImpl svec = vec(NaN);
        VectorImpl tvec = vec(NaN);
        VectorImpl out = vec(NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);
        float sumX = 1.5f + 2.5f + 0.5f;
        assertEquals(sumX, svec.get(0), 1e-5f);

        int numObs = 3;
        agg.completeSpatial(ctx, numObs, svec);
        assertEquals(sumX / numObs, svec.get(0), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.1f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.3f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.4f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.5f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.6f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.7f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.8f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.9f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(1.0f), 1, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-5f);

        agg.completeTemporal(ctx, 10, tvec);
        assertEquals(0.77f, tvec.get(0), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(0.77f, out.get(0), 1e-5f);
    }

    @Test
    public void testAggregatorPercentileWithNaN() {
        AggregatorPercentile agg = new AggregatorPercentile(new MyVariableContext("c"), "c", "target", 50);

        VectorImpl svec = vec(NaN);
        VectorImpl tvec = vec(NaN);
        VectorImpl out = vec(NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(NaN), svec);
        float sumX = 1.5f + 2.5f;
        assertEquals(sumX, svec.get(0), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(sumX / 2, svec.get(0), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);

        agg.aggregateTemporal(ctx, vec(NaN), 1, tvec);
        agg.aggregateTemporal(ctx, vec(NaN), 1, tvec);
        agg.aggregateTemporal(ctx, vec(NaN), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.4f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.5f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.6f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.7f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(NaN), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.9f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(NaN), 1, tvec);
        assertEquals(0.0f, tvec.get(0), 1e-5f);

        agg.completeTemporal(ctx, 10, tvec);
        assertEquals(0.6f, tvec.get(0), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(0.6f, out.get(0), 1e-5f);
    }

    @Test
    public void testAggregatorPercentileWithZeroValues() {
        AggregatorPercentile agg = new AggregatorPercentile(new MyVariableContext("c"), "c", "target", 50);

        VectorImpl svec = vec(NaN);
        VectorImpl tvec = vec(NaN);
        VectorImpl out = vec(NaN);

        agg.initSpatial(ctx, svec);
        agg.completeSpatial(ctx, 0, svec);
        agg.initTemporal(ctx, tvec);
        agg.completeTemporal(ctx, 0, tvec);
        agg.computeOutput(tvec, out);
        assertTrue(Float.isNaN(out.get(0)));
    }
}
