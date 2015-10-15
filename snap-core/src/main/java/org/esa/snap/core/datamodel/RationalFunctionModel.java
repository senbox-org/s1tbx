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

package org.esa.snap.core.datamodel;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Class for approximating a function of two variables with a rational
 * function.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RationalFunctionModel implements Cloneable {

    private final int maxDegree;
    private final int termCount;

    /**
     * Coefficients for the numerator polynomial P.
     */
    private final double[] c;

    /**
     * Coefficients for the denominator polynomial Q.
     */
    private final double[] d;

    /**
     * The root mean square error of the approximation.
     */
    private final double rmse;
    /**
     * The root maximum error of the approximation.
     */
    private double maxError;

    /**
     * Constructs a rational function model for  approximating a function
     * g(x, y) with a rational function R(x, y) = P(x, y) / Q(x, y) where
     * P and Q are polynomials of up to 4th degree.
     *
     * @param degreeP the degree of the numerator polynomial P.
     * @param degreeQ the degree of the denominator polynomial Q.
     * @param x       the x-coordinates corresponding to the function values
     *                being approximated.
     * @param y       the y-coordinates corresponding to the function values
     *                being approximated.
     * @param g       the function values g(x, y) being approximated.
     *
     * @throws IllegalArgumentException if the degree of the numerator or
     *                                  denominator polynomial is greater
     *                                  than four.
     * @throws IllegalArgumentException if the lengths of x, y, and g are
     *                                  not the same.
     */
    public RationalFunctionModel(int degreeP, int degreeQ, double[] x, double[] y, double[] g) {
        this(degreeP, degreeQ, x, y, g, 0);
    }

    /**
     * Constructs a rational function model for  approximating a function
     * g(x, y) with a rational function R(x, y) = P(x, y) / Q(x, y) where
     * P and Q are polynomials of up to 4th degree.
     *
     * @param degreeP        the degree of the numerator polynomial P.
     * @param degreeQ        the degree of the denominator polynomial Q.
     * @param x              the x-coordinates corresponding to the function
     *                       values being approximated.
     * @param y              the y-coordinates corresponding to the function
     *                       values being approximated.
     * @param g              the function values g(x, y) being approximated.
     * @param iterationCount the number of iterations carried out to refine
     *                       the initial polynomial coefficients.
     *
     * @throws IllegalArgumentException if the degree of the numerator or
     *                                  denominator polynomial is greater
     *                                  than four or less than 0.
     * @throws IllegalArgumentException if the lengths of x, y, and g are
     *                                  not the same.
     */
    RationalFunctionModel(int degreeP, int degreeQ, double[] x, double[] y, double[] g, int iterationCount) {
        if (degreeP < 0) {
            throw new IllegalArgumentException("degreeP < 0");
        }
        if (degreeQ < 0) {
            throw new IllegalArgumentException("degreeQ < 0");
        }
        if (degreeP > 4) {
            throw new IllegalArgumentException("degreeP > 4");
        }
        if (degreeQ > 4) {
            throw new IllegalArgumentException("degreeQ > 4");
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
        if (x.length != g.length) {
            throw new IllegalArgumentException("x.length != g.length");
        }

        final int termCountP = getTermCountP(degreeP);
        final int termCountQ = getTermCountQ(degreeQ);

        maxDegree = Math.max(degreeP, degreeQ);
        termCount = Math.max(termCountP, termCountQ + 1);
        c = new double[termCountP];
        d = new double[termCountQ];

        // calculate terms
        final double[][] terms = new double[x.length][];
        for (int i = 0; i < x.length; i++) {
            terms[i] = getTerms(x[i], y[i]);
        }

        // calculate polynomial coefficients
        fit(terms, g, c, d);
        for (int iter = 0; iter < iterationCount; iter++) {
            refineFit(terms, g, c, d);
        }

        rmse = rmse(x, y, g);
        maxError = maxError(x, y, g);
    }

    public static int getTermCountP(int degreeP) {
        return ((degreeP + 1) * (degreeP + 2)) / 2;
    }

    public static int getTermCountQ(int degreeQ) {
        return ((degreeQ + 1) * (degreeQ + 2)) / 2 - 1;
    }

    /**
     * Returns the rational function value approximating g(x, y).
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     *
     * @return the rational function value R(x, y).
     */
    public double getValue(double x, double y) {
        final double[] terms = getTerms(x, y);
        return innerProduct(c, terms, 0.0, 0) / innerProduct(d, terms, 1.0, 1);
    }

    /**
     * Returns the root mean square error (RMSE) of the approximation.
     *
     * @return the root mean square error.
     */
    public double getRmse() {
        return rmse;
    }

    /**
     * Returns the maximum error of the approximation.
     *
     * @return the maximum error.
     */
    public double getMaxError() {
        return maxError;
    }

    private double[] getTerms(double x, double y) {
        final double[] terms = new double[termCount];

        terms[0] = 1.0;
        if (maxDegree > 0) {
            terms[1] = x;
            terms[2] = y;
        }
        if (maxDegree > 1) {
            terms[3] = x * x;
            terms[4] = x * y;
            terms[5] = y * y;
        }
        if (maxDegree > 2) {
            terms[6] = x * terms[3];
            terms[7] = x * terms[4];
            terms[8] = x * terms[5];
            terms[9] = y * terms[5];
        }
        if (maxDegree > 3) {
            terms[10] = x * terms[6];
            terms[11] = x * terms[7];
            terms[12] = x * terms[8];
            terms[13] = x * terms[9];
            terms[14] = y * terms[9];
        }

        return terms;
    }

    private static double innerProduct(double[] a, double[] terms, double init, int firstTerm) {
        double s = init;
        for (int i = 0, j = firstTerm; i < a.length; i++, j++) {
            s += a[i] * terms[j];
        }
        return s;
    }

    private static void fit(double[][] terms, double[] g, double[] c, double[] d) {
        assert (g.length == terms.length);

        final int l = c.length;
        final int r = d.length;
        final int m = g.length;
        final int n = l + r;

        // build the design matrix
        final double[][] a = new double[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(terms[i], 0, a[i], 0, l);
            System.arraycopy(terms[i], 1, a[i], l, r);
            for (int j = l; j < n; j++) {
                a[i][j] *= -g[i];
            }
        }
        // solve the linear equation
        final double[] x = new double[n];
        solve(a, g, x);
        // set the polynomial coefficients
        System.arraycopy(x, 0, c, 0, l);
        System.arraycopy(x, l, d, 0, r);
    }

    private static void refineFit(double[][] terms, double[] g, double[] c, double[] d) {
        assert (g.length == terms.length);

        final int l = c.length;
        final int r = d.length;
        final int m = g.length;
        final int n = l + r;

        // build the design matrix and the right hand side of the linear equation
        final double[][] a = new double[m][n];
        final double[] b = new double[m];
        for (int i = 0; i < m; i++) {
            final double w = innerProduct(d, terms[i], 1.0, 1);
            final double h = innerProduct(c, terms[i], 0.0, 0) / w;

            System.arraycopy(terms[i], 0, a[i], 0, l);
            System.arraycopy(terms[i], 1, a[i], l, r);
            for (int j = l; j < n; j++) {
                a[i][j] *= -h;
            }
            for (int j = 0; j < n; j++) {
                a[i][j] /= w;
            }
            b[i] = g[i] - h;
        }
        // solve the linear equation
        final double[] x = new double[n];
        solve(a, b, x);
        // update the polynomial coefficients
        for (int j = 0; j < l; j++) {
            c[j] += x[j];
        }
        for (int j = 0; j < r; j++) {
            d[j] += x[l + j];
        }
    }

    private double rmse(double[] x, double[] y, double[] g) {
        double sum = 0.0;
        for (int i = 0; i < g.length; i++) {
            final double d = getValue(x[i], y[i]) - g[i];
            sum += d * d;
        }
        return Math.sqrt(sum / g.length);
    }

    private double maxError(double[] x, double[] y, double[] g) {
        double maxError = 0.0;
        for (int i = 0; i < g.length; i++) {
            final double d = Math.abs(getValue(x[i], y[i]) - g[i]);
            if (d > maxError) {
                maxError = d;
            }
        }
        return maxError;
    }

    private static void solve(double[][] a, double[] b, double[] x) {
        final int m = b.length;
        final int n = x.length;

        final SingularValueDecomposition svd;
        final Matrix u;
        final Matrix v;

        if (m < n) {
            svd = new Matrix(a, m, n).transpose().svd();
            u = svd.getV();
            v = svd.getU();
        } else {
            svd = new Matrix(a, m, n).svd();
            u = svd.getU();
            v = svd.getV();
        }

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

    public String createCFunctionCode(String compute_x, String lat, String lon) {
        return null;
    }
}
