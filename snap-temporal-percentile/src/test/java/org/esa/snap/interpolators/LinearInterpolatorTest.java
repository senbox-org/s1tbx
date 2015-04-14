package org.esa.snap.interpolators;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LinearInterpolatorTest {

    private LinearInterpolator linearInterpolator;

    @Before
    public void setUp() throws Exception {
        linearInterpolator = new LinearInterpolator();
    }

    @Test
    public void testInterpolationOfStraightLine() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{1d, 1d, 1d, 1d, 1d};

        //execution
        final InterpolatingFunction function = linearInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);
        assertUnknownValuesAreGuessedCorrectly(y, function);
    }

    @Test
    public void testInterpolationOfFunctionWithPositiveAndNegativeSlope() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{1d, 3d, 5d, 3d, 1d};

        //execution
        final InterpolatingFunction function = linearInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);
        assertUnknownValuesAreGuessedCorrectly(y, function);
    }

    @Test
    public void testInterpolationOfQuadraticFunction() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{Math.pow(0d, 2), Math.pow(2d, 2), Math.pow(4d, 2), Math.pow(6d, 2), Math.pow(8d, 2)};

        //execution
        final InterpolatingFunction function = linearInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);
        assertUnknownValuesAreGuessedCorrectly(y, function);
    }

    @Test
    public void testInterpolationOfSinusFunction() {
        //preparation
        double[] x = new double[]{0, 2, 4, 6, 8};
        double[] y = new double[]{Math.sin(0d), Math.sin(2d), Math.sin(4d), Math.sin(6d), Math.sin(8d)};

        //execution
        final InterpolatingFunction function = linearInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);
        assertUnknownValuesAreGuessedCorrectly(y, function);
    }

    @Test
    public void testInterpolationOfRandomValues() {
        double[] x = new double[]{0d, 4d, 7d, 8d, 10d, 14d, 16d};
        double[] y = new double[]{2d, 1d, 8d, 5d, 4d, 1d, 7d};
        double[] testValues = new double[]{2d, 5.5, 7.5, 9d, 12d, 15d};

        final InterpolatingFunction function = linearInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);
        assertUnknownValuesAreGuessedCorrectly(y, function, testValues);
    }

    private void assertKnownValuesAreAssignedCorrectly(double[] x, double[] y, InterpolatingFunction function) {
        for (int i = 0; i < x.length; i++) {
            assertEquals(y[i], function.value(x[i]), 1e-10);
        }
    }

    private void assertUnknownValuesAreGuessedCorrectly(double[] y, InterpolatingFunction function) {
        double[] testValues = new double[]{1d, 3d, 5d, 7d};
        assertUnknownValuesAreGuessedCorrectly(y, function, testValues);
    }

    private void assertUnknownValuesAreGuessedCorrectly(double[] y, InterpolatingFunction function, double[] testValues) {
        for (int i = 0; i < testValues.length; i++) {
            final double middleBetweenTwoValues = (y[i + 1] + y[i]) / 2;
            assertEquals(middleBetweenTwoValues, function.value(testValues[i]), 1e-10);
        }
    }

}
