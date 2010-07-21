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

package org.esa.beam.dataio.chris.internal;

import java.util.Arrays;

/**
 * The class {@code MaskRefinement} encapsulates the mask refinement
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MaskRefinement {

    private double acceptanceThreshold;
    private double acceptanceThresholdSquareRoot;

    /**
     * Constructor.
     *
     * @param acceptanceThreshold the acceptance threshold.
     */
    public MaskRefinement(double acceptanceThreshold) {
        setAcceptanceThreshold(acceptanceThreshold);
    }

    /**
     * Returns the acceptance threshold.
     *
     * @return the acceptance threshold.
     */
    public final double getAcceptanceThreshold() {
        return acceptanceThreshold;
    }

    /**
     * Sets the acceptance threshold.
     *
     * @param threshold the acceptance threshold.
     */
    public final void setAcceptanceThreshold(double threshold) {
        acceptanceThreshold = threshold;
        acceptanceThresholdSquareRoot = Math.sqrt(threshold);
    }

    /**
     * Refines the mask associated with the given radiance raster data.
     *
     * @param rciData     the radiance raster data.
     * @param maskData    the mask raster data. On output holds the refined mask.
     * @param rasterWidth the number of raster data columns.
     */
    public void refine(int[] rciData, short[] maskData, int rasterWidth) {
        final double[] hf = new double[rasterWidth - 1];
        final double[] lf = new double[rasterWidth / 2 - 1];

        for (int i = 0; i < maskData.length; i += rasterWidth) {
            adjacentDifference(rciData, i, hf);
            adjacentDifferenceEven(rciData, i, lf);

            if (median(hf) > median(lf) * acceptanceThresholdSquareRoot) {
                // mark all pixels in odd raster columns as dropout noise
                for (int j = 0; j < rasterWidth; j += 2) {
                    maskData[i + j] = 1;
                }
            }
        }
    }

    private static void adjacentDifference(final int[] values, final int offset, final double[] diffs) {
        adjacentDifference(values, offset, 1, diffs);
    }

    private static void adjacentDifferenceEven(final int[] values, final int offset, final double[] diffs) {
        adjacentDifference(values, offset + 1, 2, diffs);
    }

    /**
     * Returns the absolute difference for adjacent elements of an array or a
     * slice of an array. Here, a slice is defined by the offset of its first
     * element and the stride to reach the next element.
     *
     * @param values the array.
     * @param offset the offset.
     * @param stride the stride.
     * @param diffs  the absolute differences.
     */
    static void adjacentDifference(int[] values, int offset, int stride, double[] diffs) {
        for (int i = offset, j = 0; j < diffs.length; i += stride, ++j) {
            diffs[j] = Math.abs(values[i + stride] - values[i]);
        }
    }

    /**
     * Returns the median of an array of {@code int} values.
     *
     * @param values the values.
     *
     * @return the median value.
     *
     * @throws IllegalArgumentException if {@code values} is empty.
     * @throws NullPointerException     if {@code values} is {@code null}.
     */
    static double median(double[] values) {
        if (values == null) {
            throw new NullPointerException("values == null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values.length == 0");
        }

        return Sorter.nthElement(Arrays.copyOf(values, values.length), values.length >> 1);
    }

}
