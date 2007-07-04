package org.esa.beam.dataio.chris;

import java.util.Arrays;

/**
 * The class {@code MaskRefinement} encapsulates the mask refinement
 * algorithm developed by Luis Gomez Chova
 *
 * @author Ralf Quast
 * @version $Revision: 1.5 $ $Date: 2007/04/18 16:01:35 $
 */
class MaskRefinement {

    private double acceptanceThreshold;

    /**
     * Constructor.
     *
     * @param acceptanceThreshold the acceptance threshold.
     */
    public MaskRefinement(final double acceptanceThreshold) {
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
     * @param acceptanceThreshold the acceptance threshold.
     */
    public final void setAcceptanceThreshold(final double acceptanceThreshold) {
        this.acceptanceThreshold = Math.sqrt(acceptanceThreshold);
    }

    /**
     * Refines the mask associated with the given radiance raster data.
     *
     * @param data        the radiance raster data.
     * @param dataWidth   the number of radiance data raster columns.
     * @param mask        the mask raster data. May cover only part of the radiance data raster.
     * @param maskOffsetX the offset between the first radiance data and mask data raster columns.
     * @param maskOffsetY the offset between the first radiance data and mask data raster row.
     * @param maskWidth   the number of mask data raster columns.
     */
    public void perform(final int[] data, final int dataWidth,
                        final short[] mask, final int maskOffsetX, final int maskOffsetY, final int maskWidth) {
        final double [] hf = new double[dataWidth - 1];
        final double [] lf = new double[dataWidth / 2 - 1];
        final int oddColOffset = maskOffsetX % 2;

        for (int maskLineStart = 0, dataLineStart = maskOffsetY * dataWidth; maskLineStart < mask.length; maskLineStart += maskWidth, dataLineStart += dataWidth)
        {
            adjacentDifference(data, dataLineStart, hf);
            adjacentDifferenceEven(data, dataLineStart, lf);

            if (median(hf) > median(lf) * acceptanceThreshold) {
                // Mark all pixels in odd raster columns as drop-out noise
                for (int k = oddColOffset; k < maskWidth; k += 2) {
                    mask[maskLineStart + k] = 1;
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
    static void adjacentDifference(final int[] values, final int offset, final int stride, final double[] diffs) {
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
    static double median(final double[] values) {
        if (values == null) {
            throw new NullPointerException("values == null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values.length == 0");
        }

        final double[] doubles = new double[values.length];

        System.arraycopy(values, 0, doubles, 0, values.length);
        Arrays.sort(doubles);

        final int halfLength = values.length >> 1;

        if (halfLength << 1 == values.length) {
            // even
            return 0.5 * (doubles[halfLength - 1] + doubles[halfLength]);
        } else {
            // odd
            return doubles[halfLength];
        }
    }

}
