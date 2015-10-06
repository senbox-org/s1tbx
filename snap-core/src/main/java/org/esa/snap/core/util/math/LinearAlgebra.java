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

/**
 * Linear algebra - calculations and utilities for vectors and matrixes.
 * <p>
 * Note that for the purpose of performance the calculation functions do
 * no argument checking.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class LinearAlgebra {

    /**
     * Tests if a given {@code double[][]} array is a matrix.
     *
     * @param array the array.
     *
     * @return {@code true} if the given array is a matrix, {@code false} otherwise.
     */
    public static boolean isMatrix(double[][] array) {
        if (array == null || array.length == 0) {
            return false;
        }

        for (final double[] row : array) {
            if (row == null || row.length == 0) {
                return false;
            }
        }

        for (int i = 1; i < array.length; ++i) {
            if (array[i].length != array[0].length) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if a given {@code double[][]} array is a matrix with a prescribed number of
     * rows and columns.
     *
     * @param rowCount the prescribed number of rows. If zero or negative, any number of
     *                 rows is accepted.
     * @param colCount the prescribed number of columns. If zero or negative, any number
     *                 of columns is accepted.
     * @param array    the array.
     *
     * @return {@code true} if the given array is a matrix, {@code false} otherwise.
     */
    public static boolean isMatrix(int rowCount, int colCount, double[][] array) {
        final int m = Math.max(0, rowCount);
        final int n = Math.max(0, colCount);

        if (array == null || array.length != m) {
            return false;
        }

        for (final double[] row : array) {
            if (row == null || row.length != n) {
                return false;
            }
        }

        return true;
    }

    /**
     * Multiplies a vector by a scalar.
     *
     * @param b the vector to be multiplied. On return contains the result.
     * @param c the scalar.
     *
     * @return the result vector.
     */
    public static double[] multiply(final double[] b, final double c) {
        for (int i = 0; i < b.length; ++i) {
            b[i] *= c;
        }

        return b;
    }

    /**
     * Multiplies a matrix by a vector (from the right).
     *
     * @param a the matrix.
     * @param b the vector.
     *
     * @return the result vector.
     */
    public static double[] multiply(final double[][] a, final double[] b) {
        return multiply(a, b, new double[a[0].length]);
    }

    /**
     * Multiplies a matrix by a vector (from the right).
     *
     * @param a the matrix.
     * @param b the vector.
     * @param c the result vector.
     *
     * @return the result vector.
     */
    public static double[] multiply(final double[][] a, final double[] b, final double[] c) {
        for (int i = 0; i < a.length; ++i) {
            c[i] = innerProduct(a[i], b);
        }

        return c;
    }

    /**
     * Multiplies a matrix by a vector (from the left).
     *
     * @param b the vector.
     * @param a the matrix.
     *
     * @return the result vector.
     */
    public static double[] multiply(final double[] b, final double[][] a) {
        return multiply(b, a, new double[a[0].length]);
    }

    /**
     * Multiplies a matrix by a vector (from the left).
     *
     * @param b the vector.
     * @param a the matrix.
     * @param c the result vector.
     *
     * @return the result vector.
     */
    public static double[] multiply(final double[] b, final double[][] a, final double[] c) {
        for (int j = 0; j < a[0].length; ++j) {
            c[j] = innerProduct(b, a, j);
        }

        return c;
    }

    /**
     * Multiplies a matrix by a vector (from the left) and subtracts a scalar.
     *
     * @param b the vector.
     * @param a the matrix.
     * @param s the scalar.
     *
     * @return the result vector.
     */
    public static double[] multiplyAndSubtract(final double[] b, final double[][] a, final double s) {
        return multiplyAndSubtract(b, a, s, new double[a[0].length]);
    }

    /**
     * Multiplies a matrix by a vector (from the left) and subtracts a scalar.
     *
     * @param b the vector.
     * @param a the matrix.
     * @param s the scalar.
     * @param c the result vector.
     *
     * @return the result vector.
     */
    public static double[] multiplyAndSubtract(final double[] b, final double[][] a, final double s, final double[] c) {
        for (int j = 0; j < a[0].length; ++j) {
            c[j] = innerProduct(b, a, j) - s;
        }

        return c;
    }

    /**
     * Returns the inner product of two vectors.
     *
     * @param a the first vector.
     * @param b the second vector.
     *
     * @return the inner product.
     */
    public static double innerProduct(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; ++i) {
            sum += a[i] * b[i];
        }

        return sum;
    }

    /**
     * Calculates the inner product of a vector and a column in a matrix.
     *
     * @param a the vector.
     * @param b the matrix.
     * @param j the index of a column in {@code b}.
     *
     * @return the inner product of {@code a} and the ith column in {@code b}.
     */
    public static double innerProduct(final double[] a, final double[][] b, final int j) {
        double sum = 0.0;
        for (int i = 0; i < a.length; ++i) {
            sum += a[i] * b[i][j];
        }

        return sum;
    }

    /**
     * Calculates the inner product of a row in a matrix and a column in another matrix.
     *
     * @param a the first matrix.
     * @param b the second matrix.
     * @param i the index of a row in {@code a}.
     * @param j the index of a column in {@code b}.
     *
     * @return the inner product of the ith row in {@code a} and the jth column in {@code b}.
     */
    public static double innerProduct(final double[][] a, final double[][] b, final int i, final int j) {
        return innerProduct(a[i], b, j);
    }

    /**
     * Returns the outer product of two vectors.
     *
     * @param a the first vector.
     * @param b the second vector.
     *
     * @return the outer product.
     */
    public static double[][] outerProduct(double[] a, double[] b) {
        return outerProduct(a, b, new double[a.length][b.length]);
    }

    /**
     * Returns the outer product of two vectors.
     *
     * @param a the first vector.
     * @param b the second vector.
     * @param c the result vector.
     *
     * @return the result vector.
     */
    public static double[][] outerProduct(double[] a, double[] b, double[][] c) {
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b.length; ++j) {
                c[i][j] = a[i] * b[j];
            }
        }

        return c;
    }

    /**
     * Multiplies two matrixes.
     *
     * @param a the first matrix.
     * @param b the second matrix.
     *
     * @return the result matrix.
     */
    public static double[][] multiply(final double[][] a, final double[][] b) {
        return multiply(a, b, new double[a.length][b[0].length]);
    }

    /**
     * Multiplies two matrixes.
     *
     * @param a the first matrix.
     * @param b the second matrix.
     * @param c the result matrix.
     *
     * @return the result matrix.
     */
    public static double[][] multiply(final double[][] a, final double[][] b, final double[][] c) {
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b[0].length; ++j) {
                c[i][j] = innerProduct(a, b, i, j);
            }
        }

        return c;
    }

    /**
     * Subtracts the outer product of two vectors from a matrix.
     *
     * @param a the matrix. On return contains the result matrix.
     * @param b the first vector.
     * @param c the second vector.
     *
     * @return the result matrix.
     */
    public static double[][] subtract(final double[][] a, final double[] b, final double[] c) {
        final int m = b.length;
        final int n = c.length;

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                a[i][j] -= b[i] * c[j];
            }
        }

        return a;
    }
}
