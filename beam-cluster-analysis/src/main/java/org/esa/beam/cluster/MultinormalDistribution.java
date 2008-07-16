/* Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import static java.lang.Math.*;
import java.text.MessageFormat;

/**
 * Multinormal distribution.
 *
 * @author Ralf Quast
 * @version $Revision: 2221 $ $Date: 2008-06-16 11:19:52 +0200 (Mo, 16 Jun 2008) $
 */
public class MultinormalDistribution implements Distribution {

    private final double[] mean;
    private final double[][] covariances;

    private final double logNormFactor;
    private final Eigendecomposition eigendecomposition;

    /**
     * Constructs a new multinormal distribution.
     *
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances.
     */
    public MultinormalDistribution(double[] mean, double[][] covariances) {
        if (covariances.length != mean.length) {
            throw new IllegalArgumentException("covariances.length != mean.length");
        }
        for (int i = 0; i < mean.length; ++i) {
            if (covariances[i].length != mean.length) {
                throw new IllegalArgumentException(MessageFormat.format("covariances[{0}].length != mean.length", i));
            }
        }

        this.mean = mean;
        this.covariances = covariances;

        eigendecomposition = new Eigendecomposition(mean.length, covariances);

        double logDet = Math.log(product(eigendecomposition.getEigenvalues()));
        if (Double.isNaN(logDet) || Double.isInfinite(logDet)) {
            throw new ArithmeticException("Matrix is ill-conditioned.");
        }

        logNormFactor = -0.5 * (mean.length * log(2.0 * PI) + logDet);
    }

    public final double probabilityDensity(double[] y) {
        return exp(logProbabilityDensity(y));
    }

    public final double logProbabilityDensity(double[] y) {
        if (y.length != mean.length) {
            throw new IllegalArgumentException("y.length != mean.length");
        }

        return logNormFactor - 0.5 * mahalanobisSquaredDistance(y);
    }

    public double[] getMean() {
        return mean;
    }

    public double[][] getCovariances() {
        return covariances;
    }

    public double mahalanobisSquaredDistance(double[] y) {
        double u = 0.0;

        for (int i = 0; i < mean.length; ++i) {
            double d = 0.0;

            for (int j = 0; j < mean.length; ++j) {
                d += eigendecomposition.getV(i, j) * (y[j] - mean[j]);
            }

            u += (d * d) / eigendecomposition.getEigenvalue(i);
        }

        return u;
    }

    private static double product(double[] values) {
        double product = values[0];

        for (int i = 1; i < values.length; ++i) {
            product *= values[i];
        }

        return product;
    }

    private static class Eigendecomposition {

        private double[] eigenvalues;
        private double[][] v;

        private Eigendecomposition(int n, double[][] symmetricMatrix) {
            final Jama.SingularValueDecomposition svd = new Jama.Matrix(symmetricMatrix, n, n).svd();

            eigenvalues = svd.getSingularValues();
            v = svd.getV().getArray();
        }

        public double[] getEigenvalues() {
            return eigenvalues;
        }

        public final double getEigenvalue(int i) {
            return eigenvalues[i];
        }

        public final double getV(int i, int j) {
            return v[i][j];
        }
    }
}
