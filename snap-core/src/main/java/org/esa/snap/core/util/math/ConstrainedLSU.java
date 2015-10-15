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

package org.esa.snap.core.util.math;

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
