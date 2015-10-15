package org.esa.snap.core.nn;

/**
 * This class serves for defining a structure containing the output of a NN together
 * with the corresponding Jacobi matrix. An instance of this class is returned by the
 * {@link NNffbpAlphaTabFast#calcJacobi(double[])} method.
 *
 * @author K. Schiller
 *         Copyright GKSS/KOF
 *         Created on 02.12.2000
 */
public class NNCalc {
    /**
     * The vector containing the output of a {@link NNffbpAlphaTabFast}.
     */
    private double[] nnOutput;
    /**
     * The corresponding Jacobi matrix.
     */
    private double[][] jacobiMatrix;

    public double[] getNnOutput() {
        return nnOutput;
    }

    public void setNnOutput(double[] nnOutput) {
        this.nnOutput = nnOutput;
    }

    public double[][] getJacobiMatrix() {
        return jacobiMatrix;
    }

    public void setJacobiMatrix(double[][] jacobiMatrix) {
        this.jacobiMatrix = jacobiMatrix;
    }
}
