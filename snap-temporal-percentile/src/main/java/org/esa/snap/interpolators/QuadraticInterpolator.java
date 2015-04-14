package org.esa.snap.interpolators;

import java.text.MessageFormat;

public class QuadraticInterpolator implements Interpolator {

    private final int minNumPoints = 3;

    @Override
    public int getMinNumPoints() {
        return minNumPoints;
    }

    @Override
    public InterpolatingFunction interpolate(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(MessageFormat.format(
                                "Dimension mismatch {0} != {1}.", x.length, y.length));
        }
        if (x.length < minNumPoints) {
            throw new IllegalArgumentException(MessageFormat.format(
                                "{0} points are required, got only {1}.", minNumPoints, x.length));
        }
        for (int i = 0; i < x.length - 1; i++) {
            if (x[i] >= x[i + 1]) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Points {0} and {1} are not strictly increasing ({2} >= {3}).",
                        i, i + 1, x[i], x[i + 1]));
            }
        }
        int numberOfResultingFunctions = x.length - 2;
        PolynomialFunction[] polynomials = new PolynomialFunction[numberOfResultingFunctions];
        for (int row = 1; row <= numberOfResultingFunctions; row++) {
            final double[] coefficients =
                    getCoefficientsForThreePoints(x[row - 1], x[row], x[row + 1], y[row - 1], y[row], y[row + 1]);
            polynomials[row - 1] = new PolynomialFunction(coefficients);
        }
        double[] knots = new double[x.length - 1];
        System.arraycopy(x, 0, knots, 0, x.length - 2);
        knots[knots.length - 1] = x[x.length - 1];
        return new InterpolatingFunction(knots, polynomials);
    }

    private static double[] getCoefficientsForThreePoints(double x0, double x1, double x2, double y0, double y1, double y2) {
        double[][] gaussianMatrix = new double[3][3];
        gaussianMatrix[0] = new double[]{1, 0, 0};
        gaussianMatrix[1] = new double[]{1, x1 - x0, Math.pow(x1 - x0, 2)};
        gaussianMatrix[2] = new double[]{1, x2 - x0, Math.pow(x2 - x0, 2)};
        double[] referenceValues = new double[]{y0, y1, y2};
        for (int i = 0; i < 3; i++) {
            formGaussianMatrixAsUpperTriangularMatrix(gaussianMatrix, referenceValues, i);
        }
        return getCoefficients(gaussianMatrix, referenceValues);
    }

    private static void formGaussianMatrixAsUpperTriangularMatrix(double[][] gaussianMatrix, double[] referenceValues, int i) {
        for (int j = i + 1; j < 3; j++) {
            double difference = gaussianMatrix[j][i] / gaussianMatrix[i][i];
            for (int k = i; k < 3; k++) {
                gaussianMatrix[j][k] -= difference * gaussianMatrix[i][k];
            }
            referenceValues[j] -= difference * referenceValues[i];
        }
    }

    private static double[] getCoefficients(double[][] gaussianMatrix, double[] referenceValues) {
        double[] coefficients = new double[3];
        coefficients[2] = referenceValues[2] / gaussianMatrix[2][2];
        double rowSum = gaussianMatrix[1][2] * coefficients[2];
        coefficients[1] = (referenceValues[1] - rowSum) / gaussianMatrix[1][1];
        coefficients[0] = referenceValues[0];
        return coefficients;
    }

}
