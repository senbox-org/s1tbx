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
 * The class {@code MatrixLookupTable} performs the function of
 * multilinear  interpolation for matrix  lookup tables with an
 * arbitrary number of dimensions.
 * <p>
 * The implementation simply delegates to an {@link VectorLookupTable}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MatrixLookupTable {
    /**
     * The number of rows in the looked-up matrix.
     */
    private final int m;
    /**
     * The number of columns in the looked-up matrix.
     */
    private final int n;

    private final MatrixFactory matrixFactory;
    private final VectorLookupTable vectorLookupTable;

    public MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, double[] values, IntervalPartition... dimensions) {
        this(m, n, matrixFactory, new Array.Double(values), dimensions);
    }

    public MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, float[] values, IntervalPartition... dimensions) {
        this(m, n, matrixFactory, new Array.Float(values), dimensions);
    }

    public MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, double[] values, double[]... dimensions) {
        this(m, n, matrixFactory, values, IntervalPartition.createArray(dimensions));
    }

    public MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, float[] values, float[]... dimensions) {
        this(m, n, matrixFactory, values, IntervalPartition.createArray(dimensions));
    }

    private MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, Array values, IntervalPartition... dimensions) {
        this.m = m;
        this.n = n;
        this.matrixFactory = matrixFactory;

        vectorLookupTable = new VectorLookupTable(m * n, values, dimensions);
    }

    public MatrixLookupTable(int m, int n, MatrixFactory matrixFactory, VectorLookupTable vectorLookupTable) {
        this.m = m;
        this.n = n;
        this.matrixFactory = matrixFactory;
        this.vectorLookupTable = vectorLookupTable;
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return vectorLookupTable.getDimensionCount();
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    public final IntervalPartition[] getDimensions() {
        return vectorLookupTable.getDimensions();
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(final int i) {
        return vectorLookupTable.getDimension(i);
    }

    /**
     * Returns an interpolated value matrix for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @return the interpolated value matrix.
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double[][] getValues(final double... coordinates) throws IllegalArgumentException {
        return matrixFactory.createMatrix(m, n, vectorLookupTable.getValues(coordinates));
    }
}
