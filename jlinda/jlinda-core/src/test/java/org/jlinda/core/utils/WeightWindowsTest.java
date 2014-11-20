package org.jlinda.core.utils;

import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WeightWindowsTest {

    private static final double DELTA = 10e-16;

    final static int NumOfSamples = 16;
    final static double ALPHA = 0.75;
    final static double Fs = 18.96e6;
    final static double Br = 15.55e6;

    final static double deltaF = Fs / NumOfSamples;

    final static double[] hamming_EXPECTED = {0, 0, 0.573223304703363, 0.654329141908728, 0.75,
            0.845670858091272, 0.926776695296637, 0.980969883127822, 1, 0.980969883127822,
            0.926776695296637, 0.845670858091272, 0.75, 0.654329141908728, 0.573223304703363, 0};

    final static double[] inverseHamming_EXPECTED = {0, 0, 1/0.573223304703363, 1/0.654329141908728, 1/0.75,
            1/0.845670858091272, 1/0.926776695296637, 1/0.980969883127822, 1, 1/0.980969883127822,
            1/0.926776695296637, 1/0.845670858091272, 1/0.75, 1/0.654329141908728, 1/0.573223304703363, 0};
    final static double[] rect_EXPECTED = {1, 1, 0, 1, 0};

    private static double[] inputAxisHamming;
    private static double[] inputAxisRect;

    @BeforeClass
    public static void setUpTestData() {
        inputAxisHamming = MathUtils.increment(NumOfSamples,(-Fs/2), deltaF);
        inputAxisRect = new double[]{0, 0.3, 4, 0.5, -156};
    }

    @Test
    public void testRectArray() throws Exception {
        double[] rect_ACTUAL = WeightWindows.rect(inputAxisRect);
        Assert.assertArrayEquals(rect_EXPECTED, rect_ACTUAL, DELTA);
    }

    @Test
    public void testRectDoubleMatrix() throws Exception {
        DoubleMatrix rect_ACTUAL = WeightWindows.rect(new DoubleMatrix(inputAxisRect));
        Assert.assertEquals(new DoubleMatrix(rect_EXPECTED), rect_ACTUAL);
    }

    @Test
    public void testHammingArray() throws Exception {
        double[] hamming_ACTUAL = WeightWindows.hamming(inputAxisHamming, Br, Fs, ALPHA);
        Assert.assertArrayEquals(hamming_EXPECTED,hamming_ACTUAL, DELTA);
    }

    @Test
    public void testHammingDoubleMatrix() throws Exception {
        DoubleMatrix hamming_ACTUAL_MATRIX = WeightWindows.hamming(new DoubleMatrix(inputAxisHamming), Br, Fs, ALPHA);
        Assert.assertEquals(new DoubleMatrix(hamming_EXPECTED), hamming_ACTUAL_MATRIX);
    }

    @Test
    public void testInvertHammingFunction() throws Exception {
        double[] hammingWindow = WeightWindows.hamming(inputAxisHamming, Br, Fs, ALPHA);
        double[] inverseHamming_ACTUAL = WeightWindows.inverseHamming(hammingWindow);
        Assert.assertArrayEquals(inverseHamming_EXPECTED, inverseHamming_ACTUAL, DELTA);
    }

    @Test
    public void testInvertHammingArray() throws Exception {
        double[] inverseHamming_ACTUAL = WeightWindows.inverseHamming(inputAxisHamming, Br, Fs, ALPHA);
        Assert.assertArrayEquals(inverseHamming_EXPECTED,inverseHamming_ACTUAL, DELTA);
    }

    @Test
    public void testInvertHammingDoubleMatrix() throws Exception {
        DoubleMatrix inverseHamming_ACTUAL = WeightWindows.inverseHamming(new DoubleMatrix(inputAxisHamming), Br, Fs, ALPHA);
        Assert.assertEquals(new DoubleMatrix(inverseHamming_EXPECTED),inverseHamming_ACTUAL);
    }

    @Test
    public void testSimpleHamming() throws Exception {
        final int n = 8;
        double[] hammingWindow_EXPECTED = {0.08, 0.253194691144983, 0.642359629619905, 0.954445679235113, 0.954445679235113, 0.642359629619905, 0.253194691144983, 0.08};
        double[] hammingWindow_ACTUAL = WeightWindows.hamming(8);
        Assert.assertArrayEquals(hammingWindow_EXPECTED, hammingWindow_ACTUAL, DELTA);
    }

}
