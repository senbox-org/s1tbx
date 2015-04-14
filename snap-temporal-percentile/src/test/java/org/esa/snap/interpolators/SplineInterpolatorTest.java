package org.esa.snap.interpolators;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SplineInterpolatorTest {

    private SplineInterpolator splineInterpolator;

    @Before
    public void setUp() throws Exception {
        splineInterpolator = new SplineInterpolator();
    }

    @Test
    public void testInterpolationOfStraightLine() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{1d, 1d, 1d, 1d, 1d};

        //execution
        final InterpolatingFunction function = splineInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);

        assertEquals(1d, function.value(1d), 0d);
        assertEquals(1d, function.value(3d), 0d);
        assertEquals(1d, function.value(5d), 0d);
        assertEquals(1d, function.value(7d), 0d);
    }

    @Test
    public void testInterpolationOfFunctionWithPositiveAndNegativeSlope() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{1d, 3d, 5d, 3d, 1d};

        //execution
        final InterpolatingFunction function = splineInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);

        assertEquals(2d, function.value(1d), 1);
        assertEquals(4d, function.value(3d), 1);
        assertEquals(4d, function.value(5d), 1);
        assertEquals(2d, function.value(7d), 1);
    }

    @Test
    public void testInterpolationOfQuadraticFunction() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d};
        double[] y = new double[]{Math.pow(0d, 2), Math.pow(2d, 2), Math.pow(4d, 2), Math.pow(6d, 2), Math.pow(8d, 2)};

        //execution
        final InterpolatingFunction function = splineInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);

        assertEquals(Math.pow(1d, 2), function.value(1d), 0.5);
        assertEquals(Math.pow(3d, 2), function.value(3d), 2);
        assertEquals(Math.pow(5d, 2), function.value(5d), 2);
        assertEquals(Math.pow(7d, 2), function.value(7d), 2.5);
    }

    @Test
    public void testInterpolationOfSinusFunction() {
        //preparation
        double[] x = new double[]{0d, 2d, 4d, 6d, 8d, 10d, 12d, 14d, 16d};
        double[] y = new double[]{Math.sin(0d), Math.sin(Math.PI / 8), Math.sin(Math.PI / 4), Math.sin(Math.PI / 8 * 3), Math.sin(Math.PI / 2),
                Math.sin(Math.PI / 8 * 5), Math.sin(Math.PI / 4 * 3), Math.sin(Math.PI / 8 * 7), Math.sin(Math.PI)};

        //execution
        final InterpolatingFunction function = splineInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);

        assertEquals(Math.sin(Math.PI / 16), function.value(1d), 1e-4);
        assertEquals(Math.sin(Math.PI / 16 * 3), function.value(3d), 1e-2);
        assertEquals(Math.sin(Math.PI / 16 * 5), function.value(5d), 1e-1);
        assertEquals(Math.sin(Math.PI / 16 * 7), function.value(7d), 1e-1);
        assertEquals(Math.sin(Math.PI / 16 * 9), function.value(9d), 1e-1);
        assertEquals(Math.sin(Math.PI / 16 * 11), function.value(11d), 1e-1);
        assertEquals(Math.sin(Math.PI / 16 * 13), function.value(13d), 1e-1);
        assertEquals(Math.sin(Math.PI / 16 * 15), function.value(15d), 1e-2);
    }

    @Test
    public void testInterpolationOfRandomValues() {
        double[] x = new double[]{0d, 4d, 7d, 8d, 10d, 14d, 16d};
        double[] y = new double[]{2d, 1d, 8d, 5d, 4d, 1d, 7d};

        final InterpolatingFunction function = splineInterpolator.interpolate(x, y);

        //assertion
        assertKnownValuesAreAssignedCorrectly(x, y, function);

        assertEquals(-0.75, function.value(2d), 1e-2);
        assertEquals(6.23, function.value(5.5), 1e-2);
        assertEquals(6.58, function.value(7.5), 1e-2);
        assertEquals(3.91, function.value(9d), 1e-2);
        assertEquals(1.67, function.value(12d), 1e-2);
        assertEquals(3.39, function.value(15d), 1e-2);
    }

    private void assertKnownValuesAreAssignedCorrectly(double[] x, double[] y, InterpolatingFunction function) {
        for (int i = 0; i < x.length - 1; i++) {
            assertEquals(y[i], function.value(x[i]), 1e-10);
        }
    }

}
