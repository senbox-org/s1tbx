/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.util.math;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * A utility class which can be used to find approximation functions for a given dataset.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class Approximator {

    /**
     * Solves a linear equation system with each term having the form c * f(x). The method finds the coefficients
     * <code>c[0]</code> to <code>c[n-1]</code> with <code>n = f.length</code> for an approximation function y'(x) =
     * <code>c[0]*f[0](x) + c[1]*f[1](x) + c[2]*f[2](x) + ... + c[n-1]*f[n-1](x)</code> which approximates the given
     * data vector x<sub>i</sub>=<code>data[i][0]</code>, y<sub>i</sub>=<code>data[i][1]</code> with i = <code>0</code>
     * to <code>data.length-1</code>.
     *
     * @param data    an array of values of the form <code>{{x1,y1}, {x2,y2}, {x3,y3}, ...} </code>
     * @param indices the co-ordinate indices vector, determining the indices of x,y within data. If <code>null</code>
     *                then <code>indices</code> defaults to <code>{0, 1}</code>.
     * @param f       the function vector, each function has the form y=f(x)
     * @param c       the resulting coefficient vector, must have the same size as the function vector
     */
    public static void approximateFX(final double data[][],
                                     int[] indices,
                                     final FX[] f,
                                     final double[] c) {
        final int n = f.length;
        final int m = data.length;
        final double[][] a = new double[n][n];
        final double[] b = new double[n];
        double x, y;
        int i, j, k;
        int iX = 0;
        int iY = 1;
        if (indices != null) {
            iX = indices[0];
            iY = indices[1];
        }
        for (i = 0; i < n; i++) { // Rows 1..n
            for (j = i; j < n; j++) {  // Columns 1..n
                for (k = 0; k < m; k++) {
                    x = data[k][iX];
                    final double result = f[i].f(x) * f[j].f(x);
                    if (!Double.isNaN(result)) {
                        a[i][j] += result; // sum fi(x) * fj(x)
                    }
                }
            }
            // Copy, since matrix is symetric
            for (j = 0; j < i; j++) {  // Columns 1..i-1
                a[i][j] = a[j][i];
            }
            // Column n+1
            for (k = 0; k < m; k++) {
                x = data[k][iX];
                y = data[k][iY];
                final double result = y * f[i].f(x);
                if (!Double.isNaN(result)) {
                    b[i] += result; // sum y * fi(x)
                }
            }
        }
        solve2(a, b, c);
    }

    /**
     * Solves a linear equation system with each term having the form c * f(x,y). The method finds the coefficients
     * <code>c[0]</code> to <code>c[n-1]</code> with <code>n = f.length</code> for an approximation function z'(x,y) =
     * <code>c[0]*f[0](x,y) + c[1]*f[1](x,y) + c[2]*f[2](x,y) + ... + c[n-1]*f[n-1](x,y)</code> which approximates the
     * given data vector x<sub>i</sub>=<code>data[i][0]</code>, y<sub>i</sub>=<code>data[i][1]</code>,
     * z<sub>i</sub>=<code>data[i][2]</code> with i = <code>0</code> to <code>data.length-1</code>.
     *
     * @param data    an array of values of the form <code>{{x1,y1,z1}, {x2,y2,z2}, {x3,y3,z3}, ...} </code>
     * @param indices the co-ordinate indices vector, determining the indices of x,y,z within <code>data</code>. If
     *                <code>null</code> then <code>indices</code> defaults to <code>{0, 1, 2}</code>.
     * @param f       the function vector, each function has the form z=f(x,y)
     * @param c       the resulting coefficient vector, must have the same size as the function vector
     */
    public static void approximateFXY(final double[][] data,
                                      final int[] indices,
                                      final FXY[] f,
                                      final double[] c) {
        final int n = f.length;
        final int m = data.length;
        final double[][] a = new double[n][n];
        final double[] b = new double[n];
        double x, y, z;
        int iX = 0;
        int iY = 1;
        int iZ = 2;
        if (indices != null) {
            iX = indices[0];
            iY = indices[1];
            iZ = indices[2];
        }
        for (int i = 0; i < n; i++) { // Rows i=1..n
            for (int j = i; j < n; j++) {  // Columns j=1..n
                for (double[] point : data) {
                    x = point[iX];
                    y = point[iY];
                    final double result = f[i].f(x, y) * f[j].f(x, y);
                    if (!Double.isNaN(result)) {
                        a[i][j] += result;  // sum fi(x,y) * fj(x,y)
                    }
                }
            }
            // Copy, since matrix is symetric
            for (int j = 0; j < i; j++) {  // Columns j=1..i-1
                a[i][j] = a[j][i];
            }
            // Column n+1
            for (double[] point : data) {
                x = point[iX];
                y = point[iY];
                z = point[iZ];
                final double result = z * f[i].f(x, y);
                if (!Double.isNaN(result)) {
                    b[i] += result;  // sum z * fi(x,y)
                }
            }
        }
        solve2(a, b, c);
    }

    /**
     * Returns the root mean square error (RMSE) for the approximation of the given data with a function given by y'(x)
     * = <code>c[0]*f[0](x) + c[1]*f[1](x) + c[2]*f[2](x) + ... + c[n-1]*f[n-1](x)</code>.
     *
     * @param data    an array of values of the form <code>{{x1,y1}, {x2,y2}, {x3,y3}, ...} </code>
     * @param indices the co-ordinate indices vector, determining the indices of x,y within <code>data</code>. If
     *                <code>null</code> then <code>indices</code> defaults to <code>{0, 1}</code>.
     * @param f       the function vector, each function has the form y=f(x)
     * @param c       the coefficient vector, must have the same size as the function vector
     */
    public static double getRMSE(final double[][] data, int[] indices, final FX[] f, double[] c) {
        final int m = data.length;
        double x, y, d;
        double mse = 0.0;
        int iX = 0;
        int iY = 1;
        if (indices != null) {
            iX = indices[0];
            iY = indices[1];
        }
        for (double[] point : data) {
            x = point[iX];
            y = point[iY];
            d = computeY(f, c, x) - y;
            mse += d * d;
        }
        mse /= m;
        return Math.sqrt(mse);
    }


    /**
     * Returns the root mean square error (RMSE) for the approximation of the given data with a function given by
     * z(x,y) = <code>c[0]*f[0](x,y) + c[1]*f[1](x,y) + c[2]*f[2](x,y) + ... + c[n-1]*f[n-1](x,y)</code>.
     *
     * @param data    an array of values of the form <code>{{x1,y1,z1}, {x2,y2,z1}, {x3,y3,z1}, ...} </code>
     * @param indices the co-ordinate indices vector, determining the indices of x,y,z within <code>data</code>. If
     *                <code>null</code> then <code>indices</code> defaults to <code>{0, 1, 2}</code>.
     * @param f       the function vector, each function has the form z=f(x,y)
     * @param c       the coefficient vector, must have the same size as the function vector
     */
    public static double computeRMSE(final double[][] data, final int[] indices, final FXY[] f, double[] c) {
        final int m = data.length;
        double x, y, z, d;
        double mse = 0.0;
        int iX = 0;
        int iY = 1;
        int iZ = 2;
        if (indices != null) {
            iX = indices[0];
            iY = indices[1];
            iZ = indices[2];
        }
        for (double[] point : data) {
            x = point[iX];
            y = point[iY];
            z = point[iZ];
            d = FXYSum.computeZ(f, c, x, y) - z;
            mse += d * d;
        }
        mse /= m;
        return Math.sqrt(mse);
    }

    /**
     * Returns the root mean square error (RMSE) for the approximation of the given data with a function given by
     * z(x,y) = <code>c[0]*f[0](x,y) + c[1]*f[1](x,y) + c[2]*f[2](x,y) + ... + c[n-1]*f[n-1](x,y)</code>.
     *
     * @param data    an array of values of the form <code>{{x1,y1,z1}, {x2,y2,z1}, {x3,y3,z1}, ...} </code>
     * @param indices the co-ordinate indices vector, determining the indices of x,y,z within <code>data</code>. If
     *                <code>null</code> then <code>indices</code> defaults to <code>{0, 1, 2}</code>.
     * @param f       the function vector, each function has the form z=f(x,y)
     * @param c       the coefficient vector, must have the same size as the function vector
     */
    public static double[] computeErrorStatistics(final double[][] data, final int[] indices, final FXY[] f,
                                                  double[] c) {
        final int m = data.length;
        double x, y, z, d;
        double mse = 0.0;
        double emax = 0.0;
        int iX = 0;
        int iY = 1;
        int iZ = 2;
        if (indices != null) {
            iX = indices[0];
            iY = indices[1];
            iZ = indices[2];
        }
        for (double[] point : data) {
            x = point[iX];
            y = point[iY];
            z = point[iZ];
            d = FXYSum.computeZ(f, c, x, y) - z;
            emax = Math.max(emax, Math.abs(d));
            mse += d * d;
        }
        mse /= m;
        final double rmse = Math.sqrt(mse);
        return new double[]{rmse, emax};
    }

    /**
     * Computes <i>y(x) = sum(c[i] * f[i](x), i = 0, n - 1)</i>.
     *
     * @param f the function vector
     * @param c the coeffcient vector
     * @param x the x value
     * @return the y value
     */
    public static double computeY(final FX[] f, double[] c, double x) {
        final int n = f.length;
        double y = 0.0;
        for (int i = 0; i < n; i++) {
            y += c[i] * f[i].f(x);
        }
        return y;
    }

    /**
     * Computes <i>z(x,y) = sum(c[i] * f[i](x,y), i = 0, n - 1)</i>.
     *
     * @param f the function vector
     * @param c the coeffcient vector
     * @param x the x value
     * @param y the y value
     * @return the z value
     */
    public static double computeZ(final FXY[] f, double[] c, double x, double y) {
        return FXYSum.computeZ(f, c, x, y);
    }

    /**
     * Solves the matrix equation A * C = B for given A,B by performing the operation C = A.solve(B).
     *
     * @param a matrix A
     * @param b matrix B
     * @param c result matrix C
     */
    public static void solve1(final double[][] a, final double[] b, final double[] c) {
        final Matrix matrixA = new Matrix(a);
        final double[][] tempB = new double[b.length][1];
        for (int i = 0; i < tempB.length; i++) {
            tempB[i][0] = b[i];
        }
        final Matrix matrixB = new Matrix(tempB);
        final Matrix matrixC = matrixA.solve(matrixB);

        final double[][] tempC = matrixC.getArray();
        for (int i = 0; i < tempB.length; i++) {
            c[i] = tempC[i][0];
        }
    }

    /**
     * Solves the matrix equation A x = b by means of singular value decomposition.
     *
     * @param a the matrix A
     * @param b the vector b
     * @param x the solution vector x.
     */
    private static void solve2(double[][] a, double[] b, double[] x) {
        final int m = b.length;
        final int n = x.length;

        final SingularValueDecomposition svd;
        final Matrix u;
        final Matrix v;


        final Matrix matrix = new Matrix(a, m, n);
        final double det = matrix.det();
        if (det == 0.0 || Double.isNaN(det) || Double.isInfinite(det)) {
            throw new ArithmeticException("Expected an invertible matrix, but matrix is degenerate: det = " + det);
        }

        svd = matrix.svd();
        u = svd.getU();
        v = svd.getV();

        final double[] s = svd.getSingularValues();
        final int rank = svd.rank();

        for (int j = 0; j < rank; j++) {
            x[j] = 0.0;
            for (int i = 0; i < m; i++) {
                x[j] += u.get(i, j) * b[i];
            }
            s[j] = x[j] / s[j];
        }
        for (int j = 0; j < n; j++) {
            x[j] = 0.0;
            for (int i = 0; i < rank; i++) {
                x[j] += v.get(j, i) * s[i];
            }
        }
    }
}
