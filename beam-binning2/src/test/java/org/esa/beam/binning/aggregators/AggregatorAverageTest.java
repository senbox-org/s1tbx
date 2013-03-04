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

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.MyVariableContext;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static java.lang.Math.sqrt;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.createCtx;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.obsNT;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.vec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AggregatorAverageTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorAverage agg = new AggregatorAverage(new MyVariableContext("c"), "c", 0.0, null);

        assertEquals("AVG", agg.getName());
        assertTrue(Float.isNaN(agg.getOutputFillValue()));

        String[] spatialFeatureNames = agg.getSpatialFeatureNames();
        assertEquals(2, spatialFeatureNames.length);
        assertEquals("c_sum", spatialFeatureNames[0]);
        assertEquals("c_sum_sq", spatialFeatureNames[1]);

        String[] temporalFeatureNames = agg.getTemporalFeatureNames();
        assertEquals(3, temporalFeatureNames.length);
        assertEquals("c_sum", temporalFeatureNames[0]);
        assertEquals("c_sum_sq", temporalFeatureNames[1]);
        assertEquals("c_weights", temporalFeatureNames[2]);

        String[] outputFeatureNames = agg.getOutputFeatureNames();
        assertEquals(2, outputFeatureNames.length);
        assertEquals("c_mean", outputFeatureNames[0]);
        assertEquals("c_sigma", outputFeatureNames[1]);
    }

    @Test
    public void testMetadata_ForDedicatedFillValue() {
        AggregatorAverage agg = new AggregatorAverage(new MyVariableContext("c"), "c", 0.0, 43.21f);

        assertEquals("AVG", agg.getName());
        assertEquals(43.21f, agg.getOutputFillValue(), 1e-5f);
    }

    @Test
    public void testAggregatorAverageNoWeight() {
        AggregatorAverage agg = new AggregatorAverage(new MyVariableContext("c"), "c", 0.0, null);

        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(0.0f, svec.get(1), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);
        float sumX = 1.5f + 2.5f + 0.5f;
        float sumXX = 1.5f * 1.5f + 2.5f * 2.5f + 0.5f * 0.5f;
        assertEquals(sumX, svec.get(0), 1e-5f);
        assertEquals(sumXX, svec.get(1), 1e-5f);

        int numObs = 3;
        agg.completeSpatial(ctx, numObs, svec);
        assertEquals(sumX / numObs, svec.get(0), 1e-5f);
        assertEquals(sumXX / numObs, svec.get(1), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.3f, 0.09f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 2, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f, 0.04f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 7, tvec);
        assertEquals(0.3f + 0.1f + 0.2f + 0.1f, tvec.get(0), 1e-5f);
        assertEquals(0.09f + 0.01f + 0.04f + 0.01f, tvec.get(1), 1e-5f);
        assertEquals(4f, tvec.get(2), 1e-5f);

        float mean = (0.3f + 0.1f + 0.2f + 0.1f) / 4f;
        float sigma = (float) sqrt((0.09f + 0.01f + 0.04f + 0.01f) / 4f - mean * mean);
        agg.computeOutput(tvec, out);
        assertEquals(mean, out.get(0), 1e-5f);
        assertEquals(sigma, out.get(1), 1e-5f);
    }

    @Test
    public void testAggregatorAverageWeighted() {
        Aggregator agg = new AggregatorAverage(new MyVariableContext("c"), "c", 1.0, null);

        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(0.0f, svec.get(1), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);
        float sumX = 1.5f + 2.5f + 0.5f;
        float sumXX = 1.5f * 1.5f + 2.5f * 2.5f + 0.5f * 0.5f;
        assertEquals(sumX, svec.get(0), 1e-5f);
        assertEquals(sumXX, svec.get(1), 1e-5f);

        int numObs = 3;
        agg.completeSpatial(ctx, numObs, svec);
        assertEquals(sumX / numObs, svec.get(0), 1e-5f);
        assertEquals(sumXX / numObs, svec.get(1), 1e-5f);

        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(0.3f, 0.09f), 3, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 2, tvec);
        agg.aggregateTemporal(ctx, vec(0.2f, 0.04f), 1, tvec);
        agg.aggregateTemporal(ctx, vec(0.1f, 0.01f), 7, tvec);
        assertEquals(3 * 0.3f + 2 * 0.1f + 1 * 0.2f + 7 * 0.1f, tvec.get(0), 1e-5f);
        assertEquals(3 * 0.09f + 2 * 0.01f + 1 * 0.04f + 7 * 0.01f, tvec.get(1), 1e-5f);
        assertEquals(3f + 2f + 1f + 7f, tvec.get(2), 1e-5f);

        float mean = (3 * 0.3f + 2 * 0.1f + 1 * 0.2f + 7 * 0.1f) / (3f + 2f + 1f + 7f);
        float sigma = (float) sqrt((3 * 0.09f + 2 * 0.01f + 1 * 0.04f + 7 * 0.01f) / (3f + 2f + 1f + 7f) - mean * mean);
        agg.computeOutput(tvec, out);
        assertEquals(mean, out.get(0), 1e-5f);
        assertEquals(sigma, out.get(1), 1e-5f);
    }

    @Test
    public void testSuperSampling() {
        Aggregator agg = new AggregatorAverage(new MyVariableContext("c"), "c", null, null);
        VectorImpl svec = vec(NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN);

        agg.initSpatial(ctx, svec);
        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);

        agg.aggregateSpatial(ctx, obsNT(1.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(2.5f), svec);
        agg.aggregateSpatial(ctx, obsNT(0.5f), svec);

        float sumX = (1.5f + 2.5f + 0.5f) * 3;
        float sumXX = (1.5f * 1.5f + 2.5f * 2.5f + 0.5f * 0.5f) * 3;
        assertEquals(sumX, svec.get(0), 1e-5f);
        assertEquals(sumXX, svec.get(1), 1e-5f);

        int numObs = 9;
        agg.completeSpatial(ctx, numObs, svec);
        assertEquals(sumX / numObs, svec.get(0), 1e-5f);
        assertEquals(sumXX / numObs, svec.get(1), 1e-5f);
    }


}
