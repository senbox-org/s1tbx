package org.esa.beam.statistics.percentile.interpolated;

import static org.junit.Assert.*;

import org.junit.*;

public class GapFillerTest_fillStartAndEndWithFallback {

    private final float xx = Float.NaN;

    private BandConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = new BandConfiguration();
        configuration.startValueFallback = 2.2;
        configuration.endValueFallback = 4.1;
    }

    @Test
    public void testThatNothingIsChangedIfThereAreValues() {
        float[] input = {0, 0, 0, 0, 0};
        float[] expected = {0, 0, 0, 0, 0};

        GapFiller.fillStartAndEndWithFallback(input, configuration);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatStartAndEndValueIsSetIfValueIsNAN() {
        float[] input = {xx, xx, xx, xx, xx};
        float[] expected = {2.2f, xx, xx, xx, 4.1f};

        GapFiller.fillStartAndEndWithFallback(input, configuration);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatStartValueIsSetIfValueIsNAN() {
        float[] input = {xx, 2, 3, 4, 5};
        float[] expected = {2.2f, 2, 3, 4, 5};

        GapFiller.fillStartAndEndWithFallback(input, configuration);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatEndValueIsSetIfValueIsNAN() {
        float[] input = {6, 3, 4, 5, xx};
        float[] expected = {6, 3, 4, 5, 4.1f};

        GapFiller.fillStartAndEndWithFallback(input, configuration);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }
}
