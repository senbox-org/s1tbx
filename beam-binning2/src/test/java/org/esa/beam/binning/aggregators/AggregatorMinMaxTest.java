/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.createCtx;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.obsNT;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.vec;
import static org.junit.Assert.assertEquals;

public class AggregatorMinMaxTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorMinMax agg = new AggregatorMinMax(new MyVariableContext("a"), "a", null);

        assertEquals("MIN_MAX", agg.getName());

        assertEquals(2, agg.getSpatialFeatureNames().length);
        assertEquals("a_min", agg.getSpatialFeatureNames()[0]);
        assertEquals("a_max", agg.getSpatialFeatureNames()[1]);

        assertEquals(2, agg.getTemporalFeatureNames().length);
        assertEquals("a_min", agg.getTemporalFeatureNames()[0]);
        assertEquals("a_max", agg.getTemporalFeatureNames()[1]);

        assertEquals(2, agg.getOutputFeatureNames().length);
        assertEquals("a_min", agg.getOutputFeatureNames()[0]);
        assertEquals("a_max", agg.getOutputFeatureNames()[1]);
    }

    @Test
    public void tesAggregatorMinMax() {
        AggregatorMinMax agg = new AggregatorMinMax(new MyVariableContext("a"), "a", null);

        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN);
        VectorImpl out = vec(NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(Float.POSITIVE_INFINITY, svec.get(0), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, svec.get(1), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(7.3f), svec);
        agg.aggregateSpatial(ctx, obsNT(5.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(-0.1f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.0f), svec);
        assertEquals(-0.1f, svec.get(0), 1e-5f);
        assertEquals(7.3f, svec.get(1), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(-0.1f, svec.get(0), 1e-5f);
        assertEquals(7.3f, svec.get(1), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.POSITIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(1), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.9f, 1.0f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 5.1f), 5, tvec);
        agg.aggregateTemporal(ctx, vec(0.6f, 2.0f), 9, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f, 1.5f), 2, tvec);
        assertEquals(0.1f, tvec.get(0), 1e-5f);
        assertEquals(5.1f, tvec.get(1), 1e-5f);

        agg.computeOutput(tvec, out);
        assertEquals(0.1f, tvec.get(0), 1e-5f);
        assertEquals(5.1f, tvec.get(1), 1e-5f);
    }

}
