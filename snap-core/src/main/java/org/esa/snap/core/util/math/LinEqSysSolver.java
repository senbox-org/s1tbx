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

import org.esa.snap.core.util.Debug;


/**
 * A gauss-based solver for linear equation systems.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class LinEqSysSolver {


    /**
     * Solves a linear equation system using the gaussian algorithm.
     *
     * @param a   coefficient matrix [0..n-1][0..n-1] (left side of equation)
     * @param c   the constant vector [0..n-1] (right side of equation)
     * @param x   the result vector [0..n-1] (right side of equation)
     * @param n   number of coefficients
     * @param eps the epsilon value, e.g. 1.0e-09
     *
     * @throws SingularMatrixException if the matrix <code>a</code> is singular
     */
    public static boolean gauss(final double[][] a,
                                final double[] c,
                                final double[] x,
                                final int n,
                                final double eps) throws SingularMatrixException {
        int i, j, k;
        double t;

        for (k = 0; k < n - 1; ++k) {
            if (!pivotize(a, c, k, n, eps)) {
                throw new SingularMatrixException();
            }
            for (i = k + 1; i < n; ++i) {
                t = -a[i][k] / a[k][k];
                for (j = k + 1; j < n; ++j) {
                    a[i][j] += t * a[k][j];
                }
                c[i] += t * c[k];
            }
        }

        for (i = 0; i < n; i++) {
            x[i] = 0.0;
        }

        x[n - 1] = c[n - 1] / a[n - 1][n - 1];
        for (i = n - 2; i >= 0; --i) {
            t = 0.0;
            for (j = i + 1; j < n; j++) {
                t += a[i][j] * x[j];
            }
            x[i] += (c[i] - t) / a[i][i];
        }

        return true;
    }

    /**
     * Same as the <code>gausss</code> method, but prints extra debug information.
     *
     * @param a   coefficient matrix [0..n-1][0..n-1] (left side of equation)
     * @param c   the constant vector [0..n-1] (right side of equation)
     * @param x   the result vector [0..n-1] (right side of equation)
     * @param n   number of coefficients
     * @param eps the epsilon value, e.g. 1.0e-09
     *
     * @throws SingularMatrixException if the matrix <code>a</code> is singular
     * @see #gauss
     */
    public static boolean gauss_debug(double[][] a, double[] c, double[] x, int n, double eps) {
        Debug.trace("LinEqSysSolver: before gauss() call");
        Debug.trace("LinEqSysSolver: a", a);
        Debug.trace("LinEqSysSolver: c", c);
        Debug.trace("LinEqSysSolver: coeffs", x);
        boolean success = gauss(a, c, x, n, eps);
        Debug.trace("LinEqSysSolver: after gauss() call");
        Debug.trace("LinEqSysSolver: a", a);
        Debug.trace("LinEqSysSolver: c", c);
        Debug.trace("LinEqSysSolver: coeffs", x);
        return success;
    }


    /**
     * Rearranges the matrix <code>a</code> by swapping line <code>k</code> with a suitable <i>pivot</i> line <code>k'</code>.
     *
     * @param a   coefficient matrix [0..n-1][0..n-1] (left side of equation)
     * @param c   the constant vector [0..n-1] (right side of equation)
     * @param k   the current index of a diagonal element = current line and column
     * @param n   matrix size
     * @param eps the epsilon value, e.g. 1.0e-09
     *
     * @throws SingularMatrixException if the matrix <code>a</code> is singular
     */
    public static boolean pivotize(final double[][] a,
                                   final double[] c,
                                   final int k,
                                   final int n,
                                   final double eps) {
        double t;
        int i, j;

        int pivot = k;
        double maxa = abs(a[k][k]);
        for (i = k + 1; i < n; ++i) {
            t = abs(a[i][k]);
            if (t > maxa) {
                pivot = i;
                maxa = t;
            }
        }

        boolean success = true;
        if (pivot != k) {
            for (j = k; j < n - 1; ++j) {
                t = a[pivot][j];
                a[pivot][j] = a[k][j];
                a[k][j] = t;
            }
            t = c[pivot];
            c[pivot] = c[k];
            c[k] = t;
        } else if (maxa <= eps) {
            success = false;
        }

        return success;
    }


    // private, to allow the compiler inlining
    private static double abs(double x) {
        return x < 0.0 ? -x : x;
    }
}
