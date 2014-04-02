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

package org.esa.beam.binning.aggregators;


import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.MyVariableContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.*;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class AggregatorOnMaxSetWithMaskTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("mask", "b", "c"), "target", "c", "mask", "b");

        assertEquals("ON_MAX_SET_WITH_MASK", agg.getName());

        assertEquals(4, agg.getSpatialFeatureNames().length);
        assertEquals("c_max", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_mjd", agg.getSpatialFeatureNames()[1]);
        assertEquals("c_count", agg.getSpatialFeatureNames()[2]);
        assertEquals("b", agg.getSpatialFeatureNames()[3]);

        assertEquals(4, agg.getTemporalFeatureNames().length);
        assertEquals("c_max", agg.getTemporalFeatureNames()[0]);
        assertEquals("c_mjd", agg.getTemporalFeatureNames()[1]);
        assertEquals("c_count", agg.getTemporalFeatureNames()[2]);
        assertEquals("b", agg.getTemporalFeatureNames()[3]);

        assertEquals(4, agg.getOutputFeatureNames().length);
        assertEquals("target_max", agg.getOutputFeatureNames()[0]);
        assertEquals("target_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("target_count", agg.getOutputFeatureNames()[2]);
        assertEquals("b", agg.getOutputFeatureNames()[3]);
    }

    @Test
    public void testMetadataWithEmptySetNames() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("mask", "b", "c"), "target", "c", "mask");

        assertEquals("ON_MAX_SET_WITH_MASK", agg.getName());

        assertEquals(3, agg.getSpatialFeatureNames().length);
        assertEquals("c_max", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_mjd", agg.getSpatialFeatureNames()[1]);
        assertEquals("c_count", agg.getSpatialFeatureNames()[2]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("c_max", agg.getTemporalFeatureNames()[0]);
        assertEquals("c_mjd", agg.getTemporalFeatureNames()[1]);
        assertEquals("c_count", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getOutputFeatureNames().length);
        assertEquals("target_max", agg.getOutputFeatureNames()[0]);
        assertEquals("target_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("target_count", agg.getOutputFeatureNames()[2]);
    }

    @Test
    public void testAggregation() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("a", "c", "mask"), "t", "c", "mask", "a");

        VectorImpl svec = vec(NaN, NaN, NaN, NaN);   // c_max, c_mjd, c_count, a
        VectorImpl tvec = vec(NaN, NaN, NaN, NaN);   // c_max, c_mjd, c_count, a
        VectorImpl out = vec(NaN, NaN, NaN, NaN);    // t_max, t_mjd, t_count, a

        agg.initSpatial(ctx, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(0, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        Observation obs1 = obs(4, 1, 0.5f, 1);  // mjd, a, c, mask
        Observation obs2 = obs(5, 2, 0.8f, 0);  // mjd, a, c, mask
        agg.aggregateSpatial(ctx, obs1, svec);
        agg.aggregateSpatial(ctx, obs2, svec);
        assertEquals(0.5f, svec.get(0), 1e-5f);
        assertEquals(4, svec.get(1), 1e-5f);
        assertEquals(1, svec.get(2), 1e-5f);
        assertEquals(1, svec.get(3), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(0.5f, svec.get(0), 1e-5f);
        assertEquals(4, svec.get(1), 1e-5f);
        assertEquals(1, svec.get(2), 1e-5f);
        assertEquals(1, svec.get(3), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(0f, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        VectorImpl svec1 = vec(0.6f, 4, 3, 9.7f); // c_max, c_mjd, c_count, a
        VectorImpl svec2 = vec(0.3f, 5, 2, 9.8f); // c_max, c_mjd, c_count, a
        agg.aggregateTemporal(ctx, svec1, 3, tvec);
        agg.aggregateTemporal(ctx, svec2, 3, tvec);
        assertEquals(0.6f, tvec.get(0), 1e-5f);
        assertEquals(4, tvec.get(1), 1e-5f);
        assertEquals(3 + 2, tvec.get(2), 1e-5f);
        assertEquals(9.7f, tvec.get(3), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(0.6f, out.get(0), 1e-5f);
        assertEquals(4, out.get(1), 1e-5f);
        assertEquals(3 + 2, out.get(2), 1e-5f);
        assertEquals(9.7f, out.get(3), 1e-5f);
    }

    @Test
    public void testAggregation_AllNaN() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("a", "c", "mask"), "t", "c", "mask", "a");

        VectorImpl svec = vec(NaN, NaN, NaN, NaN);   // c_max, c_mjd, c_count, a
        VectorImpl tvec = vec(NaN, NaN, NaN, NaN);   // c_max, c_mjd, c_count, a
        VectorImpl out = vec(NaN, NaN, NaN, NaN);    // t_max, t_mjd, t_count, a

        agg.initSpatial(ctx, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(0, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);

        Observation obs1 = obs(4, 1, NaN, 1);  // mjd, a, c, mask
        Observation obs2 = obs(5, 2, NaN, 0);  // mjd, a, c, mask
        agg.aggregateSpatial(ctx, obs1, svec);
        agg.aggregateSpatial(ctx, obs2, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 1e-5f);
        assertEquals(NaN, svec.get(1), 1e-5f);
        assertEquals(1, svec.get(2), 1e-5f);
        assertEquals(NaN, svec.get(3), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(0), 1e-5f);
        assertEquals(NaN, svec.get(1), 1e-5f);
        assertEquals(1, svec.get(2), 1e-5f);
        assertEquals(NaN, svec.get(3), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(0f, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        agg.aggregateTemporal(ctx, vec(Float.NEGATIVE_INFINITY, NaN, 1, NaN), 3, tvec);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(NaN, tvec.get(1), 0.0f);
        assertEquals(1, tvec.get(2), 0.0f);
        assertEquals(NaN, tvec.get(3), 0.0f);

        agg.computeOutput(tvec, out);
        assertEquals(NaN, out.get(0), 1e-5f);
        assertEquals(NaN, out.get(1), 1e-5f);
        assertEquals(1, out.get(2), 1e-5f);
        assertEquals(NaN, out.get(3), 1e-5f);
    }

}
