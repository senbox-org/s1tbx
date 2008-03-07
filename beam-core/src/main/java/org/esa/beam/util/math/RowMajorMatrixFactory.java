/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.util.math;

/**
 * Matrix factory implementation.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RowMajorMatrixFactory implements MatrixFactory {

    /**
     * Creates a matrix from a given array of values, assuming
     * a row-major layout of the latter.
     *
     * @param m      the number of rows in the matrix being created.
     * @param n      the number of columns in the matrix being created.
     * @param values the values.
     * @return the matrix created from the values array.
     */
    public double[][] createMatrix(int m, int n, double[] values) {
        final double[][] matrix = new double[m][n];

        for (int i = 0, j = 0; i < m; ++i, j += n) {
            System.arraycopy(values, j, matrix[i], 0, n);
        }

        return matrix;
    }
}
