package org.esa.beam.binning.aggregators;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.MyVariableContext;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.beam.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.assertEquals;

public class AggregatorOnMaxSetTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorOnMaxSet agg = new AggregatorOnMaxSet(new MyVariableContext("a", "b", "c"), "c", "a", "b");

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
        assertEquals("c_max", agg.getOutputFeatureNames()[0]);
        assertEquals("c_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("a", agg.getOutputFeatureNames()[2]);
        assertEquals("b", agg.getOutputFeatureNames()[3]);

    }

    @Test
    public void testAggregatorOnMaxSet() {
        AggregatorOnMaxSet agg = new AggregatorOnMaxSet(new MyVariableContext("a", "b", "c"), "c", "a", "b");

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
}
