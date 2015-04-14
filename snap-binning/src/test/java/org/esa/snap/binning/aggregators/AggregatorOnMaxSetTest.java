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

public class AggregatorOnMaxSetTest {

    private BinContext ctx;
    private AggregatorOnMaxSet agg;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
        agg = new AggregatorOnMaxSet(new MyVariableContext("a", "b", "c"), "c", "Out", "a", "b");
    }

    @Test
    public void testMetadata() {
        assertEquals("ON_MAX_SET", agg.getName());

        assertEquals(4, agg.getSpatialFeatureNames().length);
        assertEquals("c_max", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_mjd", agg.getSpatialFeatureNames()[1]);
        assertEquals("a", agg.getSpatialFeatureNames()[2]);
        assertEquals("b", agg.getSpatialFeatureNames()[3]);

        assertEquals(4, agg.getTemporalFeatureNames().length);
        assertEquals("c_max", agg.getTemporalFeatureNames()[0]);
        assertEquals("c_mjd", agg.getTemporalFeatureNames()[1]);
        assertEquals("a", agg.getTemporalFeatureNames()[2]);
        assertEquals("b", agg.getTemporalFeatureNames()[3]);

        assertEquals(4, agg.getOutputFeatureNames().length);
        assertEquals("Out_max", agg.getOutputFeatureNames()[0]);
        assertEquals("Out_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("a", agg.getOutputFeatureNames()[2]);
        assertEquals("b", agg.getOutputFeatureNames()[3]);

    }

    @Test
    public void testAggregatorOnMaxSet() {
        VectorImpl svec = vec(NaN, NaN, NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(NaN, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        agg.aggregateSpatial(ctx, obs(4, 7.3f, 0.5f, 1.1f), svec);
        agg.aggregateSpatial(ctx, obs(5, 0.1f, 2.5f, 1.5f), svec);
        agg.aggregateSpatial(ctx, obs(6, 5.5f, 4.9f, 1.4f), svec);
        assertEquals(1.5f, svec.get(0), 1e-5f);
        assertEquals(5f, svec.get(1), 1e-5f);
        assertEquals(0.1f, svec.get(2), 1e-5f);
        assertEquals(2.5f, svec.get(3), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(1.5f, svec.get(0), 1e-5f);
        assertEquals(5f, svec.get(1), 1e-5f);
        assertEquals(0.1f, svec.get(2), 1e-5f);
        assertEquals(2.5f, svec.get(3), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(NaN, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.3f, 4, 0.2f, 9.7f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(1.1f, 5, 0.1f, 0.3f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(4.7f, 6, 0.6f, 7.1f), 3, tvec);
        assertEquals(4.7f, tvec.get(0), 1e-5f);
        assertEquals(6f, tvec.get(1), 1e-5f);
        assertEquals(0.6f, tvec.get(2), 1e-5f);
        assertEquals(7.1f, tvec.get(3), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(4.7f, out.get(0), 1e-5f);
        assertEquals(6f, out.get(1), 1e-5f);
        assertEquals(0.6f, out.get(2), 1e-5f);
        assertEquals(7.1f, out.get(3), 1e-5f);
    }

    @Test
    public void testAggregatorOnMaxSet_AllNaN() {
        VectorImpl svec = vec(NaN, NaN, NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(NaN, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        agg.aggregateSpatial(ctx, obs(4, 7.3f, 0.5f, Float.NaN), svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(NaN, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(NaN, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(NaN, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        agg.aggregateTemporal(ctx, vec(Float.NaN, 4, 0.2f, 9.7f), 3, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(NaN, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        agg.computeOutput(tvec, out);
        assertEquals(NaN, out.get(0), 0.0f);
        assertEquals(NaN, out.get(1), 0.0f);
        assertEquals(NaN, out.get(2), 0.0f);
        assertEquals(NaN, out.get(3), 0.0f);
    }
}
