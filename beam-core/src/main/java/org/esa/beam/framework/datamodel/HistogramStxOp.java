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

package org.esa.beam.framework.datamodel;

import org.esa.beam.util.math.DoubleList;

import javax.media.jai.UnpackedImageData;

/**
 * Utility class for calculating a histogram.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @since BEAM 4.5.1
 */
final class HistogramStxOp extends StxOp {

    private final double lowValue;
    private final double highValue;
    private final boolean logScaled;
    private final int[] bins;

    HistogramStxOp(int numBins, double lowValue, double highValue, boolean logScaled) {
        super("Histogram");
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.logScaled = logScaled;
        this.bins = new int[numBins];
    }

    int[] getBins() {
        return bins;
    }

    @Override
    public void accumulateData(UnpackedImageData dataPixels,
                               UnpackedImageData maskPixels) {

        // Do not change this code block without doing the same changes in SummaryStxOp.java
        // {{ Block Start

        final DoubleList values = StxOp.asDoubleList(dataPixels);

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

        final Transformer t = logScaled ? new LogTransformer(this.lowValue) : new IdentityTransformer();

        final int[] bins = this.bins;
        final double lowValue = t.transform(this.lowValue);
        final double highValue = t.transform(this.highValue);
        final double binWidth = (highValue - lowValue) / bins.length;

        for (int y = 0; y < height; y++) {
            int dataPixelOffset = dataLineOffset;
            int maskPixelOffset = maskLineOffset;
            for (int x = 0; x < width; x++) {
                if (mask == null || mask[maskPixelOffset] != 0) {
                    final double value = t.transform(values.getDouble(dataPixelOffset));
                    if (value >= lowValue && value <= highValue) {
                        int i = (int) ((value - lowValue) / binWidth);
                        i = i == bins.length ? i - 1 : i;
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

    private interface Transformer {
        double transform(double x);
    }

    private static final class IdentityTransformer implements Transformer {
        @Override
        public double transform(double x) {
            return x;
        }
    }

    private static final class LogTransformer implements Transformer {
        private final double bias;

        private LogTransformer(double x0) {
            this.bias = 1.0 - x0;
        }

        @Override
        public double transform(double x) {
            double tx = x + bias;
            if (tx <= 0.0) {
                return Double.NaN;
            }
            return Math.log10(tx);
        }
    }

}
