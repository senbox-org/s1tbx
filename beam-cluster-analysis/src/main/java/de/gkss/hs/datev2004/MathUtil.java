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
