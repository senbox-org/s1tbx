package org.esa.beam.util.math;

import Jama.Matrix;

import java.util.Arrays;

/**
 * Performs a constrained linear spectral unmixing, where the sum
 * of abundances always is equal to unity.
 *
 * @author Ralf Quast
 * @author Helmut Schiller (GKSS)
 * @version $Revision$ $Date$
 * @since 4.1
 */
public class ConstrainedLSU extends UnconstrainedLSU {

    private final double[] z;
    private final double[] a;

    /**
     * Constructs a new instance of this class.
     *
     * @param endmembers the endmembers, where
     *                   number of rows = number of spectral channels
     *                   number of cols = number of endmember spectra
     */
    public ConstrainedLSU(double[][] endmembers) {
        super(endmembers);

        final Matrix matrix = new Matrix(endmembers);
        final double[][] inverseAtA = matrix.transpose().times(matrix).inverse().getArrayCopy();

        z = new double[endmembers[0].length];
        Arrays.fill(z, 1.0);

        a = LinearAlgebra.multiply(inverseAtA, z);
        LinearAlgebra.multiply(a, 1.0 / LinearAlgebra.innerProduct(LinearAlgebra.multiply(z, inverseAtA), z));
    }

    @Override
    public double[][] unmix(double[][] spectra) {
        final double[][] abundances = super.unmix(spectra);

        return LinearAlgebra.subtract(abundances, a, LinearAlgebra.multiplyAndSubtract(z, abundances, 1.0));
    }
}
