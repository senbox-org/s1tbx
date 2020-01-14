package org.esa.snap.core.dataio.geocoding.util;

import java.util.Arrays;

public class SplineInterpolator {

    /**
     * Calculates the second derivative at the center of a length-3 y vector, assuming x to run from 0->2.
     * Simplified version of the Numerical Recipes code
     *
     * @param y the three y-Values
     * @return the second derivative at the position 1
     */
    static double getSecondDerivative(double[] y) {
        final double u = y[2] - 2.0 * y[1] + y[0];
        return 3.0 * u;
    }

    /**
     * Calculates a spline interpolation at the location x_int. Simplified version of Numerical Recipes code, streamlined
     * to process three points. X-locations are assumed to run 0 -> 2
     *
     * @param y     vector of y-values
     * @param deriv second derivative at the center point
     * @param x_int x-position to interpolate
     * @return the interpolated value
     */
    public static double interpolate(double[] y, double deriv, double x_int) {
        final int xHi;
        final int xLo;
        final double derivHi;
        final double derivLo;

        if (x_int > 1.0) {
            xHi = 2;
            xLo = 1;
            derivHi = 0.0;
            derivLo = deriv;
        } else {
            xHi = 1;
            xLo = 0;
            derivHi = deriv;
            derivLo = 0.0;
        }

        final double a = (xHi - x_int);
        final double b = (x_int - xLo);

        return a * y[xLo] + b * y[xHi] + ((a * a * a - a) * derivLo + (b * b * b - b) * derivHi) / 6.0;
    }

    public static double interpolate2d(double[] data, double[] derivatives, double x, double y) {
        final double[] z_int = new double[3];

        z_int[0] = interpolate(Arrays.copyOfRange(data, 0, 3), derivatives[0], x);
        z_int[1] = interpolate(Arrays.copyOfRange(data, 3, 6), derivatives[1], x);
        z_int[2] = interpolate(Arrays.copyOfRange(data, 6, 9), derivatives[2], x);

        final double secondDerivative = getSecondDerivative(z_int);

        //noinspection SuspiciousNameCombination
        return interpolate(z_int, secondDerivative, y);
    }

    public static double interpolate2d(double[][] data, double x, double y) {
        final double[] z_int = new double[3];

        double secondDerivative;
        for (int i = 0; i < 3; i++) {
            secondDerivative = getSecondDerivative(data[i]);
            z_int[i] = interpolate(data[i], secondDerivative, x);
        }

        secondDerivative = getSecondDerivative(z_int);
        //noinspection SuspiciousNameCombination
        return interpolate(z_int, secondDerivative, y);
    }

    /**
     * Calculates the second derivatives at the sampling points.
     * Implementation as in Numerical Recipes in C.
     *
     * @param x the x-Values of the sampling points
     * @param y the y-Values of the sampling points
     * @return the second derivatives
     */

    static double[] getSecondDerivatives(double[] x, double[] y) {
        final int len = y.length;
        final double[] y2 = new double[len];
        final double[] u = new double[len - 1];

        for (int i = 1; i < len - 1; i++) {
            final double sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1]);
            final double p = sig * y2[i - 1] + 2.0;
            y2[i] = (sig - 1.0) / p;
            u[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1]);
            u[i] = (6.0 * u[i] / (x[i + 1] - x[i - 1])) - sig * u[i - 1] / p;
        }

        final double qn = 0.0;
        final double un = 0.0;

        y2[len - 1] = (un - qn * u[len - 2]) / (qn * y2[len - 2] + 1.0);
        for (int k = len - 2; k > 0; k--) {
            y2[k] = y2[k] * y2[k + 1] + u[k];
        }
        return y2;
    }

    /**
     * Calculates a spline interpolation at the location x_int. Coded from Numerical Recipes.
     *
     * @param x     vector of x locations
     * @param y     vextor of y-values
     * @param deriv vector of second derivatives at the sampling points
     * @param x_int x-position to interpolate
     * @return the interpolated value
     */
    public static double interpolate(double[] x, double[] y, double[] deriv, double x_int) {
        final int len = x.length;

        int klo = 0;
        int khi = len - 1;
        while (khi - klo > 1) {
            int k = (klo + khi) >> 1;
            if (x[k] > x_int) {
                khi = k;
            } else {
                klo = k;
            }
        }

        final double h = x[khi] - x[klo];
        final double a = (x[khi] - x_int) / h;
        final double b = (x_int - x[klo]) / h;

        return a * y[klo] + b * y[khi] + ((a * a * a - a) * deriv[klo] + (b * b * b - b) * deriv[khi]) * (h * h) / 6.0;
    }
}

