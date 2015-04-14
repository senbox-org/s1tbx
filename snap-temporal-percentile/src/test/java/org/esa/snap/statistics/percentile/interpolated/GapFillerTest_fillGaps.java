package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.interpolators.Interpolator;
import org.esa.snap.interpolators.LinearInterpolator;
import org.esa.snap.interpolators.SplineInterpolator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GapFillerTest_fillGaps {

    private final static Interpolator LINEAR = new LinearInterpolator();
    private final static Interpolator SPLINE = new SplineInterpolator();

    private final float xx = Float.NaN;
    private float startValueFallback;
    private float endValueFallback;

    @Before
    public void setUp() throws Exception {
        startValueFallback = 1.0f;
        endValueFallback = 3.0f;
    }

    @Test
    public void testThatGapsAreFilledWithoutStartAndStopReplacement() {
        float[] input = new float[]{1, xx, xx, xx, 5};
        float[] expected = new float[]{1, 2, 3, 4, 5};

        GapFiller.fillGaps(input, LINEAR, startValueFallback, endValueFallback);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testGapsFillingWithNegativeValues() {
        float[] input = {1, xx, xx, xx, xx, -7, xx, xx, xx, xx, -5};
        float[] expected = {1, -0.6f, -2.2f, -3.8f, -5.4f, -7, -6.6f, -6.2f, -5.8f, -5.4f, -5};

        GapFiller.fillGaps(input, LINEAR, startValueFallback, endValueFallback);

        assertEquals(11, input.length);
        assertArrayEquals(expected, input, 1e-6f);
    }

    @Test
    public void testThatGapsAreFilledWithStartAndStopReplacement() {
        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2, 3, 4, 5, 4.5f, 4, 3.5f, 3};

        GapFiller.fillGaps(input, LINEAR, startValueFallback, endValueFallback);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatGapsAreFilledWithLinearInterpolation() {
        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2, 3, 4, 5, 4.5f, 4, 3.5f, 3};

        GapFiller.fillGaps(input, LINEAR, startValueFallback, endValueFallback);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatGapsAreFilledWithSplineInterpolation() {
        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2.3515625f, 3.5625f, 4.4921875f, 5, 4.9921875f, 4.5625f, 3.8515625f, 3};

        GapFiller.fillGaps(input, SPLINE, startValueFallback, endValueFallback);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }
}
