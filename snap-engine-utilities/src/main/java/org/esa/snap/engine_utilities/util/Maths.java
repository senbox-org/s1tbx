/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.util;

import Jama.Matrix;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.eo.Constants;

public final class Maths {
    private Maths() {
    }

    /**
     * Perform linear interpolation.
     *
     * @param y0 First sample value.
     * @param y1 Second sample value.
     * @param mu A parameter in range [0,1] that defines the interpolated sample position between y0 and y1.
     *           A 0 value of mu corresponds to sample y0.
     * @return The interpolated sample value.
     */
    public static double interpolationLinear(final double y0, final double y1, final double mu) {
        return (1 - mu) * y0 + mu * y1;
    }

    /**
     * Perform cubic interpolation.
     *
     * @param y0 First sample value.
     * @param y1 Second sample value.
     * @param y2 Third sample value.
     * @param y3 Forth sample value.
     * @param mu A parameter in range [0,1] that defines the interpolated sample position between y1 and y2.
     *           A 0 value of mu corresponds to sample y1.
     * @return The interpolated sample value.
     */
    public static double interpolationCubic(
            final double y0, final double y1, final double y2, final double y3, final double mu) {

        return ((-0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3) * mu * (mu * mu) + (y0 - 2.5 * y1 + 2 * y2 - 0.5 * y3) * (mu * mu) + (-0.5 * y0 + 0.5 * y2) * mu + y1);
    }

    public static double interpolationCubic(
            final double y0, final double y1, final double y2, final double y3, final double mu, final double mu2, final double mu3) {

        return ((-0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3) * mu3 + (y0 - 2.5 * y1 + 2 * y2 - 0.5 * y3) * mu2 + (-0.5 * y0 + 0.5 * y2) * mu + y1);
    }

    /**
     * Perform cubic2 interpolation.
     *
     * @param y0 First sample value.
     * @param y1 Second sample value.
     * @param y2 Third sample value.
     * @param y3 Forth sample value.
     * @param mu A parameter in range [0,1] that defines the interpolated sample position between y1 and y2.
     *           A 0 value of mu corresponds to sample y1.
     * @return The interpolated sample value.
     */
    public static double interpolationCubic2(
            final double y0, final double y1, final double y2, final double y3, final double mu) {

        final double mu2 = mu * mu;
        final double a0 = y3 - y2 - y0 + y1;
        final double a1 = y0 - y1 - a0;
        final double a2 = y2 - y0;

        return (a0 * mu * mu2 + a1 * mu2 + a2 * mu + y1);
    }

    /**
     * Perform sinc interpolation.
     *
     * @param y0 First sample value.
     * @param y1 Second sample value.
     * @param y2 Third sample value.
     * @param y3 Forth sample value.
     * @param y4 Fifth sample value.
     * @param mu A parameter in range [-0.5, 0.5] that defines the interpolated sample position wrt y2.
     *           A 0 value of mu corresponds to sample y2.
     * @return The interpolated sample value.
     */
    public static double interpolationSinc(
            final double y0, final double y1, final double y2, final double y3, final double y4, final double mu) {

        final int filterLength = 5;
        final double f0 = sinc(mu + 2.0) * hanning(mu + 2.0, filterLength);
        final double f1 = sinc(mu + 1.0) * hanning(mu + 1.0, filterLength);
        final double f2 = sinc(mu + 0.0) * hanning(mu + 0.0, filterLength);
        final double f3 = sinc(mu - 1.0) * hanning(mu - 1.0, filterLength);
        final double f4 = sinc(mu - 2.0) * hanning(mu - 2.0, filterLength);
        double sum = f0 + f1 + f2 + f3 + f4;
        return (f0 * y0 + f1 * y1 + f2 * y2 + f3 * y3 + f4 * y4) / sum;
    }

    /**
     * Perform Bi-linear interpolation.
     *
     * @param v00 Sample value for pixel at (x0, y0).
     * @param v01 Sample value for pixel at (x1, y0).
     * @param v10 Sample value for pixel at (x0, y1).
     * @param v11 Sample value for pixel at (x1, y1).
     * @param muX A parameter in range [0,1] that defines the interpolated sample position between x0 and x1.
     *            A 0 value of muX corresponds to sample x0.
     * @param muY A parameter in range [0,1] that defines the interpolated sample position between y0 and y1.
     *            A 0 value of muY corresponds to sample y0.
     * @return The interpolated sample value.
     */
    public static double interpolationBiLinear(
            final double v00, final double v01, final double v10, final double v11, final double muX, final double muY) {

        //return interpolationLinear(interpolationLinear(v00, v01, muX), interpolationLinear(v10, v11, muX), muY);
        return (1 - muY) * ((1 - muX) * v00 + muX * v01) + muY * ((1 - muX) * v10 + muX * v11);
    }

    /**
     * Perform Bi-cubic interpolation.
     *
     * @param v   Array of 4x4 sample values with vij is the value for pixel at (xj, yi) where i,j = 0,1,2,3.
     * @param muX A parameter in range [0,1] that defines the interpolated sample position between x1 and x2.
     *            A 0 value of muX corresponds to sample x1.
     * @param muY A parameter in range [0,1] that defines the interpolated sample position between y1 and y2.
     *            A 0 value of muY corresponds to sample y1.
     * @return The interpolated sample value.
     */
    public static double interpolationBiCubic(final double[][] v, final double muX, final double muY) {
        //if (v.length != 4 || v[0].length != 4 || v[1].length != 4 || v[2].length != 4 || v[3].length != 4) {
        //    throw new OperatorException("Incorrect sample array length");
        //}
        final double muX2 = muX*muX;
        final double muX3 = muX*muX2;
        return interpolationCubic(interpolationCubic(v[0][0], v[0][1], v[0][2], v[0][3], muX, muX2, muX3),
                interpolationCubic(v[1][0], v[1][1], v[1][2], v[1][3], muX, muX2, muX3),
                interpolationCubic(v[2][0], v[2][1], v[2][2], v[2][3], muX, muX2, muX3),
                interpolationCubic(v[3][0], v[3][1], v[3][2], v[3][3], muX, muX2, muX3),
                muY);
    }

    /**
     * Perform Bi-cubic2 interpolation.
     *
     * @param v   Array of 4x4 sample values with vij is the value for pixel at (xj, yi) where i,j = 0,1,2,3.
     * @param muX A parameter in range [0,1] that defines the interpolated sample position between x1 and x2.
     *            A 0 value of muX corresponds to sample x1.
     * @param muY A parameter in range [0,1] that defines the interpolated sample position between y1 and y2.
     *            A 0 value of muY corresponds to sample y1.
     * @return The interpolated sample value.
     */
    public static double interpolationBiCubic2(final double[][] v, final double muX, final double muY) {
        //if (v.length != 4 || v[0].length != 4 || v[1].length != 4 || v[2].length != 4 || v[3].length != 4) {
        //    throw new OperatorException("Incorrect sample array length");
        //}
        return interpolationCubic2(interpolationCubic2(v[0][0], v[0][1], v[0][2], v[0][3], muX),
                interpolationCubic2(v[1][0], v[1][1], v[1][2], v[1][3], muX),
                interpolationCubic2(v[2][0], v[2][1], v[2][2], v[2][3], muX),
                interpolationCubic2(v[3][0], v[3][1], v[3][2], v[3][3], muX), muY);
    }

    /**
     * Perform Bi-sinc interpolation.
     *
     * @param v   Array of 5x5 sample values with vij is the value for pixel at (xj, yi) where i,j = 0,1,2,3,4.
     * @param muX A parameter in range [-0.5, 0.5] that defines the interpolated sample position wrt x2.
     *            A 0 value of mu corresponds to sample x2.
     * @param muY A parameter in range [-0.5, 0.5] that defines the interpolated sample position wrt y2.
     *            A 0 value of mu corresponds to sample y2.
     * @return The interpolated sample value.
     */
    public static double interpolationBiSinc(final double[][] v, final double muX, final double muY) {
        //if (v.length != 5 ||
        //    v[0].length != 5 || v[1].length != 5 || v[2].length != 5 || v[3].length != 5 || v[4].length != 5) {
        //    throw new OperatorException("Incorrect sample array length");
        //}
        final double tmpV0 = interpolationSinc(v[0][0], v[0][1], v[0][2], v[0][3], v[0][4], muX);
        final double tmpV1 = interpolationSinc(v[1][0], v[1][1], v[1][2], v[1][3], v[1][4], muX);
        final double tmpV2 = interpolationSinc(v[2][0], v[2][1], v[2][2], v[2][3], v[2][4], muX);
        final double tmpV3 = interpolationSinc(v[3][0], v[3][1], v[3][2], v[3][3], v[3][4], muX);
        final double tmpV4 = interpolationSinc(v[4][0], v[4][1], v[4][2], v[4][3], v[4][4], muX);
        return interpolationSinc(tmpV0, tmpV1, tmpV2, tmpV3, tmpV4, muY);
    }

    /**
     * Precalculate weight for Lagrange polynomial based interpolation.
     *
     * @param pos        Position array.
     * @param desiredPos Desired position.
     * @return The array of the weights.
     */
    public static double[] lagrangeWeight(final double pos[], final double desiredPos) {

        final int length = pos.length;
        if (desiredPos < pos[0] || desiredPos > pos[length - 1]) {
            double time = desiredPos - (int) desiredPos;
            final double[] timeArray = new double[length];
            for (int i = 0; i < length; i++) {
                timeArray[i] = pos[i] - (int) pos[i];
            }

            return computeWeight(timeArray, time);

        } else {
            return computeWeight(pos, desiredPos);
        }
    }

    private static double[] computeWeight(final double pos[], final double desiredPos) {
        final int length = pos.length;
        final double[] weight = new double[length];

        for (int i = 0; i < length; ++i) {
            double weightVal = 1;
            for (int j = 0; j < length; ++j) {
                if (j != i) {
                    weightVal *= (desiredPos - pos[j]) / (pos[i] - pos[j]);
                }
            }
            weight[i] = weightVal;
        }

        return weight;
    }

    /**
     * Perform Lagrange polynomial based interpolation.
     *
     * @param xVal   Sample value array.
     * @param yVal   Sample value array.
     * @param zVal   Sample value array.
     * @param weight the weights.
     */
    public static void lagrangeInterpolatingPolynomial(final double xVal[], final double yVal[], final double zVal[],
                                                       final double[] weight, final PosVector vector) {
        vector.x = 0;
        vector.y = 0;
        vector.z = 0;
        for (int i = 0; i < xVal.length; ++i) {
            vector.x += weight[i] * xVal[i];
            vector.y += weight[i] * yVal[i];
            vector.z += weight[i] * zVal[i];
        }
    }

    /**
     * Perform Lagrange polynomial based interpolation.
     *
     * @param pos        Position array.
     * @param val        Sample value array.
     * @param desiredPos Desired position.
     * @return The interpolated sample value.
     */
    public static double lagrangeInterpolatingPolynomial(final double pos[], final double val[], final double desiredPos) {

        double retVal = 0;
        final int length = pos.length;
        for (int i = 0; i < length; ++i) {
            double weight = 1;
            for (int j = 0; j < length; ++j) {
                if (j != i) {
                    weight *= (desiredPos - pos[j]) / (pos[i] - pos[j]);
                }
            }
            retVal += weight * val[i];
        }
        return retVal;
    }

    /**
     * Interpolate vector using 8th order Legendre interpolation.
     * <p>The method interpolates a n-dimensional vector, at desired point given as input an equidistant
     * n-dimensional vectors.
     * <p><b>Notes:</b> Coefficients for 8th order interpolation are pre-computed. Method is primarily designed for
     * interpolating orbits, and it should be used with care in other applications, although it should work anywhere.
     * <p><b>Implementation details:</b> Adapted from 'getorb' package.
     *
     * @param samples Sample value array.
     * @param x       Desired position.
     * @return The interpolated sample value.
     * @author Petar Marinkovic, PPO.labs
     */
    public static double lagrangeEightOrderInterpolation(double[] samples, double x) {

        double out = 0.0d;
        final double[] denominators = {40320, -5040, 1440, -720, 576, -720, 1440, -5040, 40320};
        final double numerator = x * (x - 1) * (x - 2) * (x - 3) * (x - 4) * (x - 5) * (x - 6) * (x - 7) * (x - 8);

        if (numerator == 0) {
            return samples[(int) Math.round(x)];
        }

        double coeff;
        for (int i = 0; i < samples.length; i++) {
            coeff = numerator / denominators[i] / (x - i);
            out += coeff * samples[i];
        }
        return out;
    }

    /**
     * Get Vandermonde matrix constructed from a given array.
     *
     * @param d                   The given range distance array.
     * @param warpPolynomialOrder The warp polynomial order.
     * @return The Vandermonde matrix.
     */
    public static Matrix createVandermondeMatrix(final double[] d, final int warpPolynomialOrder) {

        final int n = d.length;
        final double[][] array = new double[n][warpPolynomialOrder + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= warpPolynomialOrder; j++) {
                array[i][j] = Math.pow(d[i], (double) j);
            }
        }
        return new Matrix(array);
    }

    /**
     * The sinc function.
     *
     * @param x The input variable.
     * @return The sinc function value.
     */
    private static double sinc(final double x) {

        return (Double.compare(x, 0.0) == 0) ? 1.0 : FastMath.sin(x * Math.PI) / (x * Math.PI);
    }

    /**
     * The Hanning window.
     *
     * @param x            The input variable.
     * @param windowLength The window length.
     * @return The Hanning window value.
     */
    public static double hanning(final double x, final int windowLength) {

        return (x >= -0.5 * windowLength && x <= 0.5 * windowLength) ?
            0.5 * (1.0 + FastMath.cos(Constants.TWO_PI * x / (windowLength + 1))) : 0.0;
    }

    /**
     * Compute polynomial value. Given variable x and polynomial coefficients c[0], c[1], ..., c[n], this
     * function returns f(x) = c[0] + c[1]*x + ... + c[n]*x^n.
     *
     * @param x     The variable.
     * @param coeff The polynomial coefficients.
     * @return The function value.
     */
    public static double computePolynomialValue(final double x, final double[] coeff) {
        double v = 0.0;
        int i = coeff.length - 1;
        while ( i > 0) {
            v = (v + coeff[i--]) * x;
        }
        return v + coeff[0];
    }

    public static void normalizeVector(final PosVector v) {
        final double norm = Math.sqrt(innerProduct(v, v));
        v.x /= norm;
        v.y /= norm;
        v.z /= norm;
    }

    public static double innerProduct(final PosVector a, final PosVector b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    public static void crossProduct(final PosVector a, final PosVector b, final PosVector c) {

        c.x = a.y * b.z - a.z * b.y;
        c.y = a.z * b.x - a.x * b.z;
        c.z = a.x * b.y - a.y * b.x;
    }

    public static double[] polyFit(final Matrix A, final double[] y) {

        return A.solve(new Matrix(y, y.length)).getColumnPackedCopy();
    }

    public static double polyVal(final double t, final double[] coeff) {

        double val = 0.0;
        int i = coeff.length - 1;
        while ( i >= 0) {
            val = val * t + coeff[i--];
        }
        return val;
    }

}
