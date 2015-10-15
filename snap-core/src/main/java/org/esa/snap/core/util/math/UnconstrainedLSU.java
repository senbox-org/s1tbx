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

import java.text.MessageFormat;

/**
 * Performs an unconstrained linear spectral unmixing.
 *
 * @author Ralf Quast
 * @author Helmut Schiller (GKSS)
 * @version $Revision$ $Date$
 * @since 4.1
 */
public class UnconstrainedLSU implements SpectralUnmixing {

    private final double[][] endmemberMatrix;
    private final double[][] inverseEndmemberMatrix;

    /**
     * Constructs a new instance of this class.
     *
     * @param endmembers the endmembers, where
     *                   number of rows = number of spectral channels
     *                   number of cols = number of endmember spectra
     */
    public UnconstrainedLSU(double[][] endmembers) {
        if (!LinearAlgebra.isMatrix(endmembers)) {
            throw new IllegalArgumentException("Parameter 'endmembers' is not a matrix.");
        }

        endmemberMatrix = endmembers;
        inverseEndmemberMatrix = new Matrix(endmembers).inverse().getArrayCopy();
    }

    /**
     * Returns the endmembers.
     *
     * @return endmembers the endmembers, where
     *         number of rows = number of spectral channels
     *         number of cols = number of endmember spectra
     */
    public double[][] getEndmembers() {
        return endmemberMatrix;
    }

    @Override
    public double[][] unmix(double[][] spectra) {
        final int actualRowCount = spectra.length;
        final int expectedRowCount = endmemberMatrix.length;

        if (actualRowCount != expectedRowCount) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Parameter ''spectra'' is not a matrix with {0} rows.", expectedRowCount));
        }

        return LinearAlgebra.multiply(inverseEndmemberMatrix, spectra);
    }

    @Override
    public double[][] mix(double[][] abundances) {
        final int actualRowCount = abundances.length;
        final int expectedRowCount = endmemberMatrix[0].length;

        if (actualRowCount != expectedRowCount) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Parameter ''abundances'' is not a matrix with {0} rows.", expectedRowCount));
        }

        return LinearAlgebra.multiply(endmemberMatrix, abundances);
    }
}
