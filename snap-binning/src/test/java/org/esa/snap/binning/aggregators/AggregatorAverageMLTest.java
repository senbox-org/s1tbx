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
import static java.lang.Math.*;
import static org.esa.snap.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class AggregatorAverageMLTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata_noSums() {
        AggregatorAverageML agg = new AggregatorAverageML(new MyVariableContext("b"), "b", "b", 0.5, false);

        assertEquals("AVG_ML", agg.getName());

        assertEquals(2, agg.getSpatialFeatureNames().length);
        assertEquals("b_sum", agg.getSpatialFeatureNames()[0]);
        assertEquals("b_sum_sq", agg.getSpatialFeatureNames()[1]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("b_sum", agg.getTemporalFeatureNames()[0]);
        assertEquals("b_sum_sq", agg.getTemporalFeatureNames()[1]);
        assertEquals("b_weights", agg.getTemporalFeatureNames()[2]);

        assertEquals(4, agg.getOutputFeatureNames().length);
        assertEquals("b_mean", agg.getOutputFeatureNames()[0]);
        assertEquals("b_sigma", agg.getOutputFeatureNames()[1]);
        assertEquals("b_median", agg.getOutputFeatureNames()[2]);
        assertEquals("b_mode", agg.getOutputFeatureNames()[3]);
    }

    @Test
    public void testMetadata_withSums() {
        AggregatorAverageML agg = new AggregatorAverageML(new MyVariableContext("b"), "b", "b", 0.5, true);

        assertEquals("AVG_ML", agg.getName());

        assertEquals(2, agg.getSpatialFeatureNames().length);
        assertEquals("b_sum", agg.getSpatialFeatureNames()[0]);
        assertEquals("b_sum_sq", agg.getSpatialFeatureNames()[1]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("b_sum", agg.getTemporalFeatureNames()[0]);
        assertEquals("b_sum_sq", agg.getTemporalFeatureNames()[1]);
        assertEquals("b_weights", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getOutputFeatureNames().length);
        assertEquals("b_sum", agg.getOutputFeatureNames()[0]);
        assertEquals("b_sum_sq", agg.getOutputFeatureNames()[1]);
        assertEquals("b_weights", agg.getOutputFeatureNames()[2]);
    }

    @Test
    public void testAggregatorAverageML() {
        AggregatorAverageML agg = new AggregatorAverageML(new MyVariableContext("b"), "b", 0.5);

        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(0.0f, svec.get(1), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);
        assertEquals(log(1.5f) + log(2.5f) + log(0.5f), svec.get(0), 1e-5);
        assertEquals(log(1.5f) * log(1.5f) + log(2.5f) * log(2.5f) + log(0.5f) * log(0.5f), svec.get(1), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals((log(1.5f) + log(2.5f) + log(0.5f)) / sqrt(3f), svec.get(0), 1e-5f);
        assertEquals(
                (log(1.5f) * log(1.5f) + log(2.5f) * log(2.5f) + log(0.5f) * log(0.5f)) / sqrt(3f), svec.get(1), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.3f, 0.09f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 2, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f, 0.04f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 7, tvec);
        assertEquals(0.3f + 0.1f + 0.2f + 0.1f, tvec.get(0), 1e-5);
        assertEquals(0.09f + 0.01f + 0.04f + 0.01f, tvec.get(1), 1e-5);
        assertEquals(sqrt(3f) + sqrt(2f) + sqrt(1f) + sqrt(7f), tvec.get(2), 1e-5);

        agg.completeTemporal(ctx, 4, tvec);

        float mean = 1.114932F;
        float sigma = 0.119713F;
        float median = 1.108560F;
        float mode = 1.095926F;
        agg.computeOutput(tvec, out);
        assertEquals(mean, out.get(0), 1e-5f);
        assertEquals(sigma, out.get(1), 1e-5f);
        assertEquals(median, out.get(2), 1e-5f);
        assertEquals(mode, out.get(3), 1e-5f);
    }

    @Test
    public void testAggregatorAverageML_WithSums() {
        AggregatorAverageML agg = new AggregatorAverageML(new MyVariableContext("b"), "b", "b", 0.5, true);

        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(0.0f, svec.get(1), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);
        double sumX = log(1.5f) + log(2.5f) + log(0.5f);
        double sumXX = log(1.5f) * log(1.5f) + log(2.5f) * log(2.5f) + log(0.5f) * log(0.5f);
        assertEquals(sumX, svec.get(0), 1e-5);
        assertEquals(sumXX, svec.get(1), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals((log(1.5f) + log(2.5f) + log(0.5f)) / sqrt(3f), svec.get(0), 1e-5f);
        assertEquals(
                (log(1.5f) * log(1.5f) + log(2.5f) * log(2.5f) + log(0.5f) * log(0.5f)) / sqrt(3f), svec.get(1), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.3f, 0.09f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 2, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f, 0.04f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 7, tvec);
        sumX = 0.3f + 0.1f + 0.2f + 0.1f;
        sumXX = 0.09f + 0.01f + 0.04f + 0.01f;
        double sumW = sqrt(3f) + sqrt(2f) + sqrt(1f) + sqrt(7f);
        assertEquals(sumX, tvec.get(0), 1e-5);
        assertEquals(sumXX, tvec.get(1), 1e-5);
        assertEquals(sumW, tvec.get(2), 1e-5);

        agg.completeTemporal(ctx, 4, tvec);

        agg.computeOutput(tvec, out);
        assertEquals(sumX, out.get(0), 1e-5);
        assertEquals(sumXX, out.get(1), 1e-5);
        assertEquals(sumW, out.get(2), 1e-5);
    }
}
