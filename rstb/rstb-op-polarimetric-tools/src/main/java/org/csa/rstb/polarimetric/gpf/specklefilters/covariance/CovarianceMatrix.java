package org.csa.rstb.polarimetric.gpf.specklefilters.covariance;

import org.csa.rstb.polarimetric.gpf.DualPolProcessor;
import org.csa.rstb.polarimetric.gpf.QuadPolProcessor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;

public class CovarianceMatrix {
    private double Cr00, Cr01, Cr02, Cr11, Cr12, Cr22;
    private double Ci01, Ci02, Ci12;
    private int d;
    private double det;

    public CovarianceMatrix(final int matrixSize) {
        this.d = matrixSize;
        this.det = -1.0;

        if (d == 2) {
            Cr00 = 0.0;
            Cr01 = 0.0;
            Cr11 = 0.0;
            Ci01 = 0.0;
        } else if (d == 3) {
            Cr00 = 0.0;
            Cr01 = 0.0;
            Cr02 = 0.0;
            Cr11 = 0.0;
            Cr12 = 0.0;
            Cr22 = 0.0;
            Ci01 = 0.0;
            Ci02 = 0.0;
            Ci12 = 0.0;
        }
    }

    public void setCovarianceMatrix(final double[][] Cr, final double[][] Ci) {

        if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
            throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
        }

        if (d == 2) {
            Cr00 = Cr[0][0];
            Cr01 = Cr[0][1];
            Cr11 = Cr[1][1];
            Ci01 = Ci[0][1];
        } else if (d == 3) {
            Cr00 = Cr[0][0];
            Cr01 = Cr[0][1];
            Cr02 = Cr[0][2];
            Cr11 = Cr[1][1];
            Cr12 = Cr[1][2];
            Cr22 = Cr[2][2];
            Ci01 = Ci[0][1];
            Ci02 = Ci[0][2];
            Ci12 = Ci[1][2];
        }

        det = -1.0;
    }

    public double[][] getRealCovarianceMatrix() {

        final double[][] Cr = new double[d][d];
        if (d == 2) {
            Cr[0][0] = Cr00;
            Cr[0][1] = Cr01;
            Cr[1][0] = Cr01;
            Cr[1][1] = Cr11;
        } else if (d == 3) {
            Cr[0][0] = Cr00;
            Cr[0][1] = Cr01;
            Cr[0][2] = Cr02;
            Cr[1][0] = Cr01;
            Cr[1][1] = Cr11;
            Cr[1][2] = Cr12;
            Cr[2][0] = Cr02;
            Cr[2][1] = Cr12;
            Cr[2][2] = Cr22;
        }
        return Cr;
    }

    public double[][] getImagCovarianceMatrix() {

        final double[][] Ci = new double[d][d];
        if (d == 2) {
            Ci[0][0] = 0.0;
            Ci[0][1] = Ci01;
            Ci[1][0] = -Ci01;
            Ci[1][1] = 0.0;
        } else if (d == 3) {
            Ci[0][0] = 0.0;
            Ci[0][1] = Ci01;
            Ci[0][2] = Ci02;
            Ci[1][0] = -Ci01;
            Ci[1][1] = 0.0;
            Ci[1][2] = Ci12;
            Ci[2][0] = -Ci02;
            Ci[2][1] = -Ci12;
            Ci[2][2] = 0.0;
        }
        return Ci;
    }

    public void addCovarianceMatrix(final double[][] Cr, final double[][] Ci) {

        if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
            throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
        }
        if (d == 2) {
            Cr00 += Cr[0][0];
            Cr01 += Cr[0][1];
            Cr11 += Cr[1][1];
            Ci01 += Ci[0][1];
        } else if (d == 3) {
            Cr00 += Cr[0][0];
            Cr01 += Cr[0][1];
            Cr02 += Cr[0][2];
            Cr11 += Cr[1][1];
            Cr12 += Cr[1][2];
            Cr22 += Cr[2][2];
            Ci01 += Ci[0][1];
            Ci02 += Ci[0][2];
            Ci12 += Ci[1][2];
        }

        det = -1.0;
    }

    public void addWeightedCovarianceMatrix(final double w, final double[][] Cr, final double[][] Ci) {

        if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
            throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
        }
        if (d == 2) {
            Cr00 += w * Cr[0][0];
            Cr01 += w * Cr[0][1];
            Cr11 += w * Cr[1][1];
            Ci01 += w * Ci[0][1];
        } else if (d == 3) {
            Cr00 += w * Cr[0][0];
            Cr01 += w * Cr[0][1];
            Cr02 += w * Cr[0][2];
            Cr11 += w * Cr[1][1];
            Cr12 += w * Cr[1][2];
            Cr22 += w * Cr[2][2];
            Ci01 += w * Ci[0][1];
            Ci02 += w * Ci[0][2];
            Ci12 += w * Ci[1][2];
        }

        det = -1.0;
    }

    public void rescaleMatrix(final double gamma) {
        // apply to off diagonal elements only
        if (d == 2) {
            Cr01 *= gamma;
            Ci01 *= gamma;
        } else if (d == 3) {
            Cr01 *= gamma;
            Cr02 *= gamma;
            Cr12 *= gamma;
            Ci01 *= gamma;
            Ci02 *= gamma;
            Ci12 *= gamma;
        }

        det = -1.0;
    }

    public double getDeterminant() {

        if (det != -1.0) {
            return det;
        }

        if (d == 2) {
            det = Math.abs(Cr00 * Cr11 - Cr01 * Cr01 - Ci01 * Ci01);
        } else if (d == 3) {
            det = Math.abs(Cr00 * Cr11 * Cr22 - Cr00 * (Cr12 * Cr12 + Ci12 * Ci12) - Cr11 * (Cr02 * Cr02 + Ci02 * Ci02) -
                    Cr22 * (Cr01 * Cr01 + Ci01 * Ci01) + 2.0 * (Cr12 * (Cr01 * Cr02 + Ci01 * Ci02) + Ci12 * (Cr01 * Ci02 -
                    Ci01 * Cr02)));
        }

        return det;
    }

    public double[] getDiagonalElements() {

        final double[] diagonal = new double[d];
        if (d == 2) {
            diagonal[0] = Cr00;
            diagonal[1] = Cr11;
        } else if (d == 3) {
            diagonal[0] = Cr00;
            diagonal[1] = Cr11;
            diagonal[2] = Cr22;
        }
        return diagonal;
    }

    public static class C2 implements Covariance, DualPolProcessor {
        private final int matrixSize = 2;
        private double det;
        public final double[][] Cr = new double[matrixSize][matrixSize];
        public final double[][] Ci = new double[matrixSize][matrixSize];

        public void getCovarianceMatrix(final int index, final ProductData[] dataBuffers) {
            getCovarianceMatrixC2(index, dataBuffers, Cr, Ci);
            this.det = -1;
        }

        public C2 clone() {
            final C2 clone = new C2();
            clone.Cr[0][0] = Cr[0][0];
            clone.Ci[0][0] = Ci[0][0];
            clone.Cr[0][1] = Cr[0][1];
            clone.Ci[0][1] = Ci[0][1];
            clone.Cr[1][0] = Cr[1][0];
            clone.Ci[1][0] = Ci[1][0];
            clone.Cr[1][1] = Cr[1][1];
            clone.Ci[1][1] = Ci[1][1];
            return clone;
        }

        public void rescaleMatrix(final double gamma) {
            // apply to off diagonal elements only
            Cr[0][1] *= gamma;
            Ci[0][1] *= gamma;

            det = -1.0;
        }

        public void addCovarianceMatrix(final Covariance matrix) {

            final C2 c2 = (C2)matrix;
            Cr[0][0] += c2.Cr[0][0];
            Cr[0][1] += c2.Cr[0][1];
            Cr[1][1] += c2.Cr[1][1];
            Ci[0][1] += c2.Ci[0][1];

            det = -1.0;
        }

        public void addWeightedCovarianceMatrix(final double w, final Covariance matrix) {

            final C2 c2 = (C2)matrix;
            Cr[0][0] += w * c2.Cr[0][0];
            Cr[0][1] += w * c2.Cr[0][1];
            Cr[1][1] += w * c2.Cr[1][1];
            Ci[0][1] += w * c2.Ci[0][1];

            det = -1.0;
        }

        public double[][] getRealCovarianceMatrix() {
            return Cr;
        }

        public double[][] getImagCovarianceMatrix() {
            return Ci;
        }

        public double[] getDiagonalElements() {

            final double[] diagonal = new double[matrixSize];
            diagonal[0] = Cr[0][0];
            diagonal[1] = Cr[1][1];
            return diagonal;
        }

        public double getDeterminant() {

            if (det != -1.0) {
                return det;
            }

            det = Math.abs(Cr[0][0] * Cr[1][1] - Cr[0][1] * Cr[0][1] - Ci[0][1] * Ci[0][1]);

            return det;
        }
    }

    public static class C3 implements Covariance, QuadPolProcessor {
        private final int matrixSize = 3;
        private double det;
        public final double[][] Cr = new double[matrixSize][matrixSize];
        public final double[][] Ci = new double[matrixSize][matrixSize];

        public void getCovarianceMatrix(final int index, final ProductData[] dataBuffers) {
            getCovarianceMatrixC3(index, dataBuffers, Cr, Ci);
            this.det = -1;
        }

        public C3 clone() {
            final C3 clone = new C3();
            clone.Cr[0][0] = Cr[0][0];
            clone.Ci[0][0] = Ci[0][0];
            clone.Cr[0][1] = Cr[0][1];
            clone.Ci[0][1] = Ci[0][1];
            clone.Cr[0][2] = Cr[0][2];
            clone.Ci[0][2] = Ci[0][2];
            clone.Cr[1][0] = Cr[1][0];
            clone.Ci[1][0] = Ci[1][0];
            clone.Cr[1][1] = Cr[1][1];
            clone.Ci[1][1] = Ci[1][1];
            clone.Cr[1][2] = Cr[1][2];
            clone.Ci[1][2] = Ci[1][2];
            clone.Cr[2][0] = Cr[2][0];
            clone.Ci[2][0] = Ci[2][0];
            clone.Cr[2][1] = Cr[2][1];
            clone.Ci[2][1] = Ci[2][1];
            clone.Cr[2][2] = Cr[2][2];
            clone.Ci[2][2] = Ci[2][2];
            return clone;
        }

        public void rescaleMatrix(final double gamma) {
            // apply to off diagonal elements only
            Cr[0][1] *= gamma;
            Ci[0][1] *= gamma;
            Cr[0][2] *= gamma;
            Ci[0][2] *= gamma;
            Cr[1][2] *= gamma;
            Ci[1][2] *= gamma;

            det = -1.0;
        }

        public void addCovarianceMatrix(final Covariance matrix) {

            final C3 c3 = (C3)matrix;
            Cr[0][0] += c3.Cr[0][0];
            Cr[0][1] += c3.Cr[0][1];
            Cr[0][2] += c3.Cr[0][2];
            Cr[1][1] += c3.Cr[1][1];
            Cr[1][2] += c3.Cr[1][2];
            Cr[2][2] += c3.Cr[2][2];
            Ci[0][1] += c3.Ci[0][1];
            Ci[0][2] += c3.Ci[0][2];
            Ci[1][2] += c3.Ci[1][2];

            det = -1.0;
        }

        public void addWeightedCovarianceMatrix(final double w, final Covariance matrix) {

            final C3 c3 = (C3)matrix;
            Cr[0][0] += w * c3.Cr[0][0];
            Cr[0][1] += w * c3.Cr[0][1];
            Cr[0][2] += w * c3.Cr[0][2];
            Cr[1][1] += w * c3.Cr[1][1];
            Cr[1][2] += w * c3.Cr[1][2];
            Cr[2][2] += w * c3.Cr[2][2];
            Ci[0][1] += w * c3.Ci[0][1];
            Ci[0][2] += w * c3.Ci[0][2];
            Ci[1][2] += w * c3.Ci[1][2];

            det = -1.0;
        }

        public double[][] getRealCovarianceMatrix() {
            return Cr;
        }

        public double[][] getImagCovarianceMatrix() {
            return Ci;
        }

        public double[] getDiagonalElements() {

            final double[] diagonal = new double[matrixSize];
            diagonal[0] = Cr[0][0];
            diagonal[1] = Cr[1][1];
            diagonal[2] = Cr[2][2];
            return diagonal;
        }

        public double getDeterminant() {

            if (det != -1.0) {
                return det;
            }

            det = Math.abs(Cr[0][0] * Cr[1][1] * Cr[2][2] - Cr[0][0] * (Cr[1][2] * Cr[1][2] + Ci[1][2] * Ci[1][2]) - Cr[1][1] * (Cr[0][2] * Cr[0][2] + Ci[0][2] * Ci[0][2]) -
                        Cr[2][2] * (Cr[0][1] * Cr[0][1] + Ci[0][1] * Ci[0][1]) + 2.0 * (Cr[1][2] * (Cr[0][1] * Cr[0][2] + Ci[0][1] * Ci[0][2]) + Ci[1][2] * (Cr[0][1] * Ci[0][2] -
                        Ci[0][1] * Cr[0][2])));

            return det;
        }
    }
}
