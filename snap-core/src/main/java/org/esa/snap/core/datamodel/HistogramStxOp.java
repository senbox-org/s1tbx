/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.math.DoubleList;

import javax.media.jai.Histogram;
import javax.media.jai.UnpackedImageData;

/**
 * Utility class for the cumulative calculation of histograms from image data.
 *
 * @author Norman Fomferra
 * @since BEAM 4.5.1, full revision in BEAM 4.10
 */
final public class HistogramStxOp extends StxOp {

    private final Histogram histogram;
    private final Scaling scaling;

    public HistogramStxOp(int binCount, double minimum, double maximum, boolean intHistogram, boolean logHistogram) {
        super("Histogram");
        if (Double.isNaN(minimum) || Double.isInfinite(minimum)) {
            minimum = 0.0;
        }
        if (Double.isNaN(maximum) || Double.isInfinite(maximum)) {
            maximum = minimum;
        }
        scaling = Stx.getHistogramScaling(logHistogram);
        histogram = StxFactory.createHistogram(binCount, minimum, maximum, logHistogram, intHistogram);
    }

    public Histogram getHistogram() {
        return histogram;
    }

    @Override
    public void accumulateData(UnpackedImageData dataPixels,
                               UnpackedImageData maskPixels) {

        // Do not change this code block without doing the same changes in SummaryStxOp.java
        // {{ Block Start

        final DoubleList values = asDoubleList(dataPixels);

        final int dataPixelStride = dataPixels.pixelStride;
        final int dataLineStride = dataPixels.lineStride;
        final int dataBandOffset = dataPixels.bandOffsets[0];

        byte[] mask = null;
        int maskPixelStride = 0;
        int maskLineStride = 0;
        int maskBandOffset = 0;
        if (maskPixels != null) {
            mask = maskPixels.getByteData(0);
            maskPixelStride = maskPixels.pixelStride;
            maskLineStride = maskPixels.lineStride;
            maskBandOffset = maskPixels.bandOffsets[0];
        }

        final int width = dataPixels.rect.width;
        final int height = dataPixels.rect.height;

        int dataLineOffset = dataBandOffset;
        int maskLineOffset = maskBandOffset;

        // }} Block End

        final int[] bins = histogram.getBins(0);
        final double lowValue = histogram.getLowValue(0);
        final double highValue = histogram.getHighValue(0);
        final double binWidth = (highValue - lowValue) / bins.length;

        for (int y = 0; y < height; y++) {
            int dataPixelOffset = dataLineOffset;
            int maskPixelOffset = maskLineOffset;
            for (int x = 0; x < width; x++) {
                if (mask == null || mask[maskPixelOffset] != 0) {
                    final double value = scaling.scale(values.getDouble(dataPixelOffset));
                    if (value >= lowValue && value <= highValue) {
                        int i = (int) ((value - lowValue) / binWidth);
                        if (i == bins.length) {
                            i--;
                        }
                        bins[i]++;
                    }
                }
                dataPixelOffset += dataPixelStride;
                maskPixelOffset += maskPixelStride;
            }
            dataLineOffset += dataLineStride;
            maskLineOffset += maskLineStride;
        }
    }
}
