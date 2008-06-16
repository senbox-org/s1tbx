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

    private static final double MAX_CONDITION = 1.0E14;

    private final int n;
    private final double[] mean;

    private final double[][] covariances;
    private final double[][] eigenvectors;

    private final double[] eigenvalues;
    private final double logNormFactor;

    /**
     * Constructs a new multinormal distribution.
     *
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances.
     */
    public MultinormalDistribution(double[] mean, double[][] covariances) {
        this(mean.length, mean, covariances, new EigenproblemSolver());
    }

    /**
     * Constructs a new multinormal distribution.
     *
     * @param n           the dimension of the domain.
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances. Only the upper triangular
     *                    elements are used.
     * @param solver      the {@link EigenproblemSolver} used for decomposing the covariance matrix.
     */
    private MultinormalDistribution(int n, double[] mean, double[][] covariances, EigenproblemSolver solver) {
        if (mean.length != n) {
            throw new IllegalArgumentException("mean.length != n");
        }
        if (covariances.length != n) {
            throw new IllegalArgumentException("covariances.length != n");
        }
        for (int i = 0; i < n; ++i) {
            if (covariances[i].length != n) {
                throw new IllegalArgumentException(MessageFormat.format("covariances[{0}].length != n", i));
            }
        }

        this.n = n;
        this.mean = mean;
        this.covariances = covariances;

        final Eigendecomposition eigendecomposition = solver.createEigendecomposition(n, covariances);
        eigenvalues = eigendecomposition.getEigenvalues();
        eigenvectors = eigendecomposition.getV();

        // Correct eigenvalues if the covariance matrix is ill-conditioned.
        final double t = eigenvalues[0] / MAX_CONDITION - eigenvalues[n - 1];
        if (t > 0.0) {
            for (int i = 0; i < n; ++i) {
                eigenvalues[i] += t;
                covariances[i][i] += t;
            }
        }

        final double det = product(eigendecomposition.getEigenvalues());
        logNormFactor = -0.5 * (n * log(2.0 * PI) + log(det));
    }

    public final double probabilityDensity(double[] y) {
        return exp(logProbabilityDensity(y));
    }

    public final double logProbabilityDensity(double[] y) {
        if (y.length != n) {
            throw new IllegalArgumentException("y.length != n");
        }

        return logNormFactor - 0.5 * mahalanobisSquaredDistance(y);
    }

    public double[] getMean() {
        return mean;
    }

    public double[][] getCovariances() {
        return covariances;
    }

    private double mahalanobisSquaredDistance(double[] y) {
        double u = 0.0;

        for (int i = 0; i < n; ++i) {
            double d = 0.0;

            for (int j = 0; j < n; ++j) {
                d += eigenvectors[i][j] * (y[j] - mean[j]);
            }

            u += (d * d) / eigenvalues[i];
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

        public Eigendecomposition(int n, double[][] symmetricMatrix) {
            final Jama.SingularValueDecomposition svd = new Jama.Matrix(symmetricMatrix, n, n).svd();

            eigenvalues = svd.getSingularValues();
            v = svd.getV().getArray();
        }

        public final double[] getEigenvalues() {
            return eigenvalues;
        }

        public final double[][] getV() {
            return v;
        }
    }

    private static class EigenproblemSolver {

        public Eigendecomposition createEigendecomposition(int n, double[][] symmetricMatrix) {
            return new Eigendecomposition(n, symmetricMatrix);
        }
    }
}
