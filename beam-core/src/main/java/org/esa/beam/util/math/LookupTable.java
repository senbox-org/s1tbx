/*
    $Id: $

    Copyright (c) 2006 Brockmann Consult. All rights reserved. Use is
    subject to license terms.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as
    published by the Free Software Foundation; either version 2 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the BEAM software; if not, download BEAM from
    http://www.brockmann-consult.de/beam/ and install it.
*/
package org.esa.beam.util.math;

import java.text.MessageFormat;

// Keep This class even it is not yet used

/**
 * The class {@code LookupTable} performs the function of multilinear
 * interpolation for lookup tables with an arbitrary number of dimensions.
 *
 * @author Ralf Quast
 * @version $Revision: 1.3 $ $Date: 2007-06-14 17:26:28 $
 */
public class LookupTable {

    /**
     * The class {@code FracIndex} is a simple representation of
     * an index with an integral and a fractional component.
     */
    static final class FracIndex {

        /**
         * The integral component.
         */
        public int i;
        /**
         * The fractional component.
         */
        public double f;

        /**
         * Creates an array of type {@code FracIndex[]}.
         *
         * @param length the length of the array being created.
         * @return the created array.
         */
        public static FracIndex[] createArray(int length) {
            final FracIndex[] fracIndexes = new FracIndex[length];

            for (int i = 0; i < length; i++) {
                fracIndexes[i] = new FracIndex();
            }

            return fracIndexes;
        }

        /**
         * Sets the fractional component to 0.0 if it is less than
         * zero, and to 1.0 if it is greater than unity.
         */
        public final void truncate() {
            if (f < 0.0) {
                f = 0.0;
            } else if (f > 1.0) {
                f = 1.0;
            }
        }
    }

    /**
     * The lookup values.
     */
    private final double[] values;
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
     * The lookup values for the vertices bracketing the lookup point.
     */
    private final double[] v;
    /**
     * The normalized representation of the lookup point's coordinates.
     */
    private final FracIndex[] fracIndexes;

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if the length of the {@code values} array is not equal to
     *                                  the number of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final IntervalPartition... dimensions) throws
            IllegalArgumentException,
            NullPointerException {
        ensureLegalArray(dimensions);
        ensureLegalArray(values, getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;

        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = 1; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        computeVertexOffsets(strides, o);

        v = new double[1 << n];
        fracIndexes = FracIndex.createArray(n);
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if the length of the {@code values} array is is not equal to
     *                                  the number of coordinate grid vertices or any dimension is
     *                                  not an interval partion.
     * @throws NullPointerException     if the {@code values} array or the dimensions array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final double[]... dimensions) throws
            IllegalArgumentException,
            NullPointerException {
        this(values, IntervalPartition.createArray(dimensions));
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
     * Returns an interpolated value for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @return the interpolated value.
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double getValue(final double... coordinates) throws IllegalArgumentException {
        ensureLegalArray(coordinates, dimensions.length);

        for (int i = 0; i < dimensions.length; ++i) {
            computeFracIndex(dimensions[i], coordinates[i], fracIndexes[i]);
        }

        return getValue(fracIndexes);
    }

    /**
     * Returns an interpolated value for the given {@link FracIndex} coordinates (see
     * {@link #computeFracIndex} for an explanation of terms).
     *
     * @param fracIndexes the {@link FracIndex} coordinates of the lookup point.
     * @return the interpolated value.
     */
    double getValue(final FracIndex... fracIndexes) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        for (int i = 0; i < v.length; ++i) {
            v[i] = values[origin + o[i]];
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                v[j] += f * (v[m + j] - v[j]);
            }
        }

        return v[0];
    }

    /**
     * Computes the {@link FracIndex} of a coordinate value with respect to a given
     * interval partition. The integral component of the returned {@link FracIndex}
     * corresponds to the index of the maximum partition member which is less than
     * or equal to the coordinate value. The [0, 1) fractional component describes
     * the position of the coordinate value within its bracketing subinterval.
     * <p/>
     * Exception: If the given coordinate value is equal to the partition maximum,
     * the fractional component of the returned {@link FracIndex} is equal to 1.0,
     * and the integral component is set to the index of the next to last partition
     * member.
     *
     * @param partition  the interval partition.
     * @param coordinate the coordinate value. If the coordinate value is less (greater)
     *                   than the minimum (maximum) of the given interval partition,
     *                   the returned {@link FracIndex} is the same as if the coordinate.
     *                   value was equal to the partition minimum (maximum).
     * @param fracIndex  the {@link FracIndex}.
     */
    static void computeFracIndex(final IntervalPartition partition, final double coordinate,
                                 final FracIndex fracIndex) {
        int lo = 0;
        int hi = partition.getCardinal() - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < partition.get(m)) {
                hi = m;
            } else {
                lo = m;
            }
        }

        fracIndex.i = lo;
        fracIndex.f = (coordinate - partition.get(lo)) / (partition.get(hi) - partition.get(lo));
        fracIndex.truncate();
    }

    /**
     * Computes the relative array offsets of the lookup values for the vertices
     * of a coordinate grid cell.
     *
     * @param strides the strides defining the layout of the lookup value array.
     * @param offsets the offsets.
     */
    static void computeVertexOffsets(final int[] strides, final int[] offsets) {
        for (int i = 0; i < strides.length; ++i) {
            final int k = 1 << i;

            for (int j = 0; j < k; ++j) {
                offsets[k + j] = offsets[j] + strides[i];
            }
        }
    }

    /**
     * Returns the number of vertices in the coordinate grid defined by the given dimensions.
     *
     * @param dimensions the dimensions defining the coordinate grid.
     * @return the number of vertices.
     */
    static int getVertexCount(final IntervalPartition[] dimensions) {
        int count = 1;

        for (final IntervalPartition dimension : dimensions) {
            count *= dimension.getCardinal();
        }

        return count;
    }

    private static <T> void ensureLegalArray(final T[] array) throws IllegalArgumentException, NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("array.length == 0");
        }
        for (final T element : array) {
            if (element == null) {
                throw new NullPointerException("element == null");
            }
        }
    }

    private static void ensureLegalArray(final double[] array, final int length) throws
            IllegalArgumentException,
            NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
    }

}
