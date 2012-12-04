package org.esa.beam.statistics.percentile.interpolated;

import static org.junit.Assert.*;

import org.junit.*;

public class GapFillerTest_fillGaps {

    private final float xx = Float.NaN;

    private BandConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = new BandConfiguration();
        configuration.startValueFallback = 1.0;
        configuration.endValueFallback = 3.0;
    }

    @Test
    public void testThatGapsAreFilledWithoutStartAndStopReplacement() {
        float[] input = {1, xx, xx, xx, 5};
        float[] expected = {1, 2, 3, 4, 5};

        GapFiller.fillGaps(input, configuration);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testGapsFillingWithNegativeValues() {
        float[] input = {1, xx, xx, xx, xx, -7, xx, xx, xx, xx, -5};
        float[] expected = {1, -0.6f, -2.2f, -3.8f, -5.4f, -7, -6.6f, -6.2f, -5.8f, -5.4f, -5};

        GapFiller.fillGaps(input, configuration);

        assertEquals(11, input.length);
        assertArrayEquals(expected, input, 1e-6f);
    }

    @Test
    public void testThatGapsAreFilledWithStartAndStopReplacement() {
        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2, 3, 4, 5, 4.5f, 4, 3.5f, 3};

        GapFiller.fillGaps(input, configuration);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatGapsAreFilledWithLinearInterpolation() {
        configuration.interpolationMethod = "linear";

        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2, 3, 4, 5, 4.5f, 4, 3.5f, 3};

        GapFiller.fillGaps(input, configuration);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatGapsAreFilledWithSplineInterpolation() {
        configuration.interpolationMethod = "spline";

        float[] input = {xx, xx, xx, xx, 5, xx, xx, xx, xx};
        float[] expected = {1, 2.3515625f, 3.5625f, 4.4921875f, 5, 4.9921875f, 4.5625f, 3.8515625f, 3};

        GapFiller.fillGaps(input, configuration);

        assertEquals(9, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }
}
