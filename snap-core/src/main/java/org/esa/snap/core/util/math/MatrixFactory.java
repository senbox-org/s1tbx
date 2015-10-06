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
