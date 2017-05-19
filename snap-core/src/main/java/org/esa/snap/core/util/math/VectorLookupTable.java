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
 * The class {@code VectorLookupTable} performs the function of
 * multilinear  interpolation for vector lookup  tables with an
 * arbitrary number of dimensions.
 * <p>
 * todo - method for degrading a table (see {@link LookupTable})
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class VectorLookupTable {

    /**
     * The lookup values.
     */
    private final Array values;
    /**
     * The dimensions associated with the lookup table.
     */
    private final IntervalPartition[] dimensions;
    /**
     * The strides defining the layout of the lookup value array.
     */
    private final int[] strides;
    /**
     * The relative array offsets of the lookup values for the vertices of a coordinate grid cell.
     */
    private final int[] o;
    /**
     * the length of the lookup vector.
     */
    private final int vectorLength;

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final double[] values, final IntervalPartition... dimensions) {
        this(length, new Array.Double(values), dimensions);
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final float[] values, final IntervalPartition... dimensions) {
        this(length, new Array.Float(values), dimensions);
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final double[] values, final double[]... dimensions) {
        this(length, values, IntervalPartition.createArray(dimensions));
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final float[] values, final float[]... dimensions) {
        this(length, values, IntervalPartition.createArray(dimensions));
    }

    VectorLookupTable(int length, final Array values, final IntervalPartition... dimensions) {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1");
        }
        vectorLength = length;

        LookupTable.ensureLegalArray(dimensions);
        LookupTable.ensureLegalArray(values, vectorLength * LookupTable.getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;
        
        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = vectorLength; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        LookupTable.computeVertexOffsets(strides, o);

    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    public final IntervalPartition[] getDimensions() {
        return dimensions;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(final int i) {
        return dimensions[i];
    }

    /**
     * Returns an interpolated value array for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @return the interpolated value array.
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double[] getValues(final double... coordinates) throws IllegalArgumentException {
        LookupTable.ensureLegalArray(coordinates, dimensions.length);
        FracIndex[] fracIndices = FracIndex.createArray(dimensions.length);
        for (int i = 0; i < dimensions.length; ++i) {
            LookupTable.computeFracIndex(dimensions[i], coordinates[i], fracIndices[i]);
        }

        return getValues(fracIndices);
    }

    private double[] getValues(final FracIndex... fracIndexes) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        double[][] outValues = new double[1 << dimensions.length][vectorLength];
        for (int i = 0; i < outValues.length; ++i) {
            values.copyTo(origin + o[i], outValues[i], 0, outValues[i].length);
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                for (int k = 0; k < outValues[j].length; ++k) {
                    outValues[j][k] += f * (outValues[m + j][k] - outValues[j][k]);
                }
            }
        }

        return outValues[0];
    }
}
