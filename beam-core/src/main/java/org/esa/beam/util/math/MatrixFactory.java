package org.esa.beam.util.math;

/**
 * Matrix factory.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface MatrixFactory {

    /**
     * Creates a matrix from a given array of values.
     *
     * @param m      the number of rows in the matrix being created.
     * @param n      the number of columns in the matrix being created.
     * @param values the values.
     * @return the matrix created from the values array.
     */
    double[][] createMatrix(int m, int n, double[] values);
}
