package org.esa.snap.statistics.percentile.interpolated;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GapFillerTest_fillStartAndEndWithFallback {

    private final float xx = Float.NaN;
    private float startValueFallback;
    private float endValueFallback;


    @Before
    public void setUp() throws Exception {
        startValueFallback = 2.2f;
        endValueFallback = 4.1f;
    }

    @Test
    public void testThatNothingIsChangedIfThereAreValues() {
        float[] input = {0, 0, 0, 0, 0};
        float[] expected = {0, 0, 0, 0, 0};

        GapFiller.fillStartAndEndWithFallback(input, startValueFallback, endValueFallback);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatStartAndEndValueIsSetIfValueIsNAN() {
        float[] input = {xx, xx, xx, xx, xx};
        float[] expected = {2.2f, xx, xx, xx, 4.1f};

        GapFiller.fillStartAndEndWithFallback(input, startValueFallback, endValueFallback);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatStartValueIsSetIfValueIsNAN() {
        float[] input = {xx, 2, 3, 4, 5};
        float[] expected = {2.2f, 2, 3, 4, 5};

        GapFiller.fillStartAndEndWithFallback(input, startValueFallback, endValueFallback);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }

    @Test
    public void testThatEndValueIsSetIfValueIsNAN() {
        float[] input = {6, 3, 4, 5, xx};
        float[] expected = {6, 3, 4, 5, 4.1f};

        GapFiller.fillStartAndEndWithFallback(input, startValueFallback, endValueFallback);

        assertEquals(5, input.length);
        assertArrayEquals(expected, input, 1e-7f);
    }
}
