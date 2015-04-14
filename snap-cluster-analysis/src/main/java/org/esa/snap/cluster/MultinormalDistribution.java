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
package org.esa.snap.cluster;

/**
 * Multinormal distribution.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class MultinormalDistribution implements Distribution {

    private final double[] mean;
    private final double logNormFactor;

    private double[] eigenvalues;
    private double[][] v;

    /**
     * Constructs a new multinormal distribution.
     *
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances.
     */
    MultinormalDistribution(double[] mean, double[][] covariances) {
        this.mean = mean.clone();

        final Eigendecomposition decomposition = new Eigendecomposition(covariances.length, covariances);
        eigenvalues = decomposition.getEigenvalues();
        v = decomposition.getV();

        double det = eigenvalues[0];
        for (int i = 1; i < eigenvalues.length; ++i) {
            det *= eigenvalues[i];
        }
        final double logDet = Math.log(det);

        if (Double.isNaN(logDet) || Double.isInfinite(logDet)) {
            throw new ArithmeticException("Matrix is numerically singular.");
        }

        logNormFactor = -0.5 * (mean.length * Math.log(2.0 * Math.PI) + logDet);
    }

    @Override
    public final double probabilityDensity(double[] y) {
        return Math.exp(logProbabilityDensity(y));
    }

    @Override
    public final double logProbabilityDensity(double[] y) {
        if (y.length != mean.length) {
            throw new IllegalArgumentException("y.length != mean.length");
        }

        return logNormFactor - 0.5 * mahalanobisSquaredDistance(y);
    }

    private double mahalanobisSquaredDistance(double[] y) {
        double u = 0.0;

        for (int i = 0; i < mean.length; ++i) {
            double d = 0.0;

            for (int j = 0; j < mean.length; ++j) {
                d += v[i][j] * (y[j] - mean[j]);
            }

            u += (d * d) / eigenvalues[i];
        }

        return u;
    }

    // todo - replace Jama SVD with something else
    private static class Eigendecomposition {

        private double[] eigenvalues;
        private double[][] v;

        private Eigendecomposition(int n, double[][] matrix) {
            final Jama.SingularValueDecomposition svd = new Jama.Matrix(matrix, n, n).svd();

            eigenvalues = svd.getSingularValues();
            v = svd.getV().getArrayCopy();
        }

        public final double[] getEigenvalues() {
            return eigenvalues;
        }

        public final double[][] getV() {
            return v;
        }
    }
}
