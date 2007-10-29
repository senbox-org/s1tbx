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

/**
 * The class {@code IntervalPartition} is a representation of an interval partition,
 * i.e. a strictly increasing sequence of real numbers.
 *
 * @author Ralf Quast
 * @version $Revision: 1.2 $ $Date: 2007-06-19 06:22:41 $
 */
public class IntervalPartition {

    private double[] partition;

    /**
     * Constructs an interval partition from a sequence of real numbers.
     *
     * @param sequence the sequence. The sequence must increase strictly and
     *                 consist of at least two real numbers.
     * @throws IllegalArgumentException if the sequence is not strictly increasing
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the sequence is {@code null}.
     */
    public IntervalPartition(final double... sequence) {
        if (sequence == null) {
            throw new NullPointerException("sequence == null");
        }
        if (sequence.length < 2) {
            throw new IllegalArgumentException("sequence.length < 2");
        }
        ensureStrictIncrease(sequence);

        this.partition = new double[sequence.length];
        System.arraycopy(sequence, 0, this.partition, 0, sequence.length);
    }

    /**
     * Creates an array of interval partitions from an array of sequences.
     *
     * @param sequences the array of sequences. Each sequence must increase strictly
     *                  and consist of at least two real numbers.
     * @return the created array of interval partitions.
     * @throws IllegalArgumentException if the length of the sequence array is zero
     *                                  or any sequence is not strictly increasing
     *                                  or consists of less than two real numbers.
     * @throws NullPointerException     if the array of sequences or any sequence
     *                                  is {@code null}.
     */
    public static IntervalPartition[] createArray(double[]... sequences) {
        if (sequences == null) {
            throw new NullPointerException("sequences == null");
        }
        if (sequences.length == 0) {
            throw new IllegalArgumentException("sequences.length == 0");
        }

        final IntervalPartition[] partitions = new IntervalPartition[sequences.length];

        for (int i = 0; i < partitions.length; ++i) {
            partitions[i] = new IntervalPartition(sequences[i]);
        }

        return partitions;
    }

    /**
     * Returns the cardinal number of the interval partition.
     *
     * @return the cardinal number.
     */
    public final int getCardinal() {
        return partition.length;
    }

    /**
     * Returns the ith real number in the interval partition.
     *
     * @param i the index number of the real number of interest.
     * @return the ith real number.
     */
    public final double get(int i) {
        return partition[i];
    }

    /**
     * Returns the maximum (i.e. final) real number in the partition.
     *
     * @return the maximum real number in the partition.
     */
    public final double getMax() {
        return partition[partition.length - 1];
    }

    /**
     * Returns the minimum (i.e. first) real number in the partition.
     *
     * @return the minimum real number in the partition.
     */
    public final double getMin() {
        return partition[0];
    }

    /**
     * Returns the mesh of the interval partition, i.e. the maximum
     * distance between two adjacent real numbers in the partition.
     *
     * @return the mesh.
     */
    public final double getMesh() {
        double mesh = 0.0;

        for (int i = 1; i < partition.length; ++i) {
            final double length = partition[i] - partition[i - 1];
            if (length > mesh) {
                mesh = length;
            }
        }

        return mesh;
    }

    private static void ensureStrictIncrease(final double... sequence) throws IllegalArgumentException {
        for (int i = 1; i < sequence.length; ++i) {
            if (sequence[i - 1] < sequence[i]) {
                continue;
            }
            throw new IllegalArgumentException("sequence is not strictly increasing");
        }
    }
}
