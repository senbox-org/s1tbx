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

package de.gkss.hs.datev2004;

public class MathUtil {

    public static double[] matrixTimesVector(double[][] matrix, double[] vector) {
        if (matrix[0].length != vector.length) {
            throw new IllegalArgumentException("matrix[0].length != vector.length");
        }
        double[] res = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            res[i] = 0;
            for (int j = 0; j < matrix[0].length; j++) {
                res[i] = res[i] + matrix[j][i] * vector[j];
            }
        }
        return res;
    }

    public static double[] vectorSubtract(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("v1.length != v2.length");
        }
        double[] res = new double[v1.length];
        for (int i = 0; i < v1.length; i++) {
            res[i] = v1[i] - v2[i];
        }
        return res;
    }

    public static double[] vectorAdd(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("v1.length != v2.length");
        }
        double[] res = new double[v1.length];
        for (int i = 0; i < v1.length; i++) {
            res[i] = v1[i] + v2[i];
        }
        return res;
    }

    public static double[] multiplyVecorWithScalar(double[] v, double s) {
        double[] res = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            res[i] = s * v[i];
        }
        return res;
    }

    public static double scalarProduct(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("v1.length != v2.length");
        }
        double res = 0.0;
        for (int i = 0; i < v1.length; i++) {
            res = res + v1[i] * v2[i];
        }
        return res;
    }

    public static double vectorNorm(double[] v) {
        double res = 0.0;
        for (double elem : v) {
            res = res + elem * elem;
        }
        return Math.sqrt(res);
    }

}
