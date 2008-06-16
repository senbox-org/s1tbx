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

import org.esa.beam.cluster.Distribution;

import static java.lang.Math.*;
import java.text.MessageFormat;

/**
 * Multinormal distribution.
 *
 * @author Ralf Quast
 * @version $Revision: 2221 $ $Date: 2008-06-16 11:19:52 +0200 (Mo, 16 Jun 2008) $
 */
public class MultinormalDistribution implements Distribution {

    private final int n;

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

        eigendecomposition = solver.createEigendecomposition(n, covariances);
//        final double t = eigenvalues[0] / 1.0E14 - eigenvalues[n - 1];
//        if (t > 0.0) {
//            for (int i = 0; i < n; ++i) {
//                covariances[i][i] += t;
//                eigenvalues[i] += t;
//            }
//        }

        final double det = product(eigendecomposition.getEigenvalues());
        if (det <= 0.0 || Double.isNaN(det)) {
            throw new IllegalStateException("Matrix determinant det = " + det + " is not positive.");
        }
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

    private double mahalanobisSquaredDistance(double[] y) {
        double u = 0.0;

        for (int i = 0; i < n; ++i) {
            double d = 0.0;

            for (int j = 0; j < n; ++j) {
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

        public Eigendecomposition(Jama.SingularValueDecomposition svd) {
            eigenvalues = svd.getSingularValues();
            v = svd.getV().getArray();
        }

        public final double getEigenvalue(int i) {
            return eigenvalues[i];
        }

        public final double[] getEigenvalues() {
            return eigenvalues;
        }

        public final double getV(int i, int j) {
            return v[i][j];
        }
    }

    private static class EigenproblemSolver {

        public Eigendecomposition createEigendecomposition(int n, double[][] symmetricMatrix) {
            return new Eigendecomposition(new Jama.Matrix(symmetricMatrix, n, n).svd());
        }
    }
}
