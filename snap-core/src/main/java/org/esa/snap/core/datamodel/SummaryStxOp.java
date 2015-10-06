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

import javax.media.jai.UnpackedImageData;

import static java.lang.Double.*;

/**
 * Utility class for calculating minimum, maximum, mean and standard deviation. Uses
 * a one-pass algorithm for computing mean and variance.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @since BEAM 4.5.1, full revision in 4.10
 */
final public class SummaryStxOp extends StxOp {

    private double minimum;
    private double maximum;
    private double mean;
    private double meanSqr;
    private long sampleCount;

    private double valueSum;
    private double sqrSum;
    private double power4Sum;

    public SummaryStxOp() {
        super("Summary");
        this.minimum = POSITIVE_INFINITY;
        this.maximum = NEGATIVE_INFINITY;
        this.sampleCount = 0;
        this.valueSum = 0;
        this.sqrSum = 0;
        this.power4Sum = 0;
    }

    public double getMinimum() {
        // Check case in which we have never seen any data tile
        return minimum == POSITIVE_INFINITY ? NaN : minimum;
    }

    public double getMaximum() {
        // Check case in which we have never seen any data tile
        return maximum == NEGATIVE_INFINITY ? NaN : maximum;
    }

    public double getMean() {
        return sampleCount > 0 ? mean : NaN;
    }

    public double getStandardDeviation() {
        return sampleCount > 0 ? Math.sqrt(getVariance()) : NaN;
    }

    public double getVariance() {
        return sampleCount > 1 ? meanSqr / (sampleCount - 1) : sampleCount == 1 ? 0.0 : NaN;
    }

    public double getCoefficientOfVariation(final String unit) {
        double cv = 0.0;
        if (unit != null && unit.contains("intensity")) {
            final double m = valueSum / sampleCount;
            final double m2 = sqrSum / sampleCount;
            cv = Math.sqrt(m2 - m*m) / m;
        } else {
            final double m4 = power4Sum / sampleCount;
            final double m2 = sqrSum / sampleCount;
            cv = Math.sqrt(m4 - m2*m2) / m2;
        }
        return cv;
    }

    public double getEquivalentNumberOfLooks(final String unit) {
        double enl = 0.0;
        if (unit != null && unit.contains("intensity")) {
            final double m = valueSum / sampleCount;
            final double m2 = sqrSum / sampleCount;
            final double mm = m*m;
            enl = mm / (m2 - mm);
        } else {
            final double m4 = power4Sum / sampleCount;
            final double m2 = sqrSum / sampleCount;
            final double m2m2 = m2*m2;
            enl = m2m2 / (m4 - m2m2);
        }
        return enl;
    }

    @Override
    public void accumulateData(UnpackedImageData dataPixels,
                               UnpackedImageData maskPixels) {

        // Do not change this code block without doing the same changes in HistogramStxOp.java
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

        double tileMinimum = this.minimum;
        double tileMaximum = this.maximum;
        long tileSampleCount = this.sampleCount;
        double tileMean = this.mean;
        double tileMeanSqr = this.meanSqr;

        double tmpValueSum = this.valueSum;
        double tmpSqrSum = this.sqrSum;
        double tmpPower4Sum = this.power4Sum;

        double value, delta;
        for (int y = 0; y < height; y++) {
            int dataPixelOffset = dataLineOffset;
            int maskPixelOffset = maskLineOffset;
            for (int x = 0; x < width; x++) {
                if (mask == null || mask[maskPixelOffset] != 0) {

                    value = values.getDouble(dataPixelOffset);
                    if(!Double.isInfinite(value) && !Double.isNaN(value)) {

                    tileSampleCount++;
                    if (value < tileMinimum) {
                        tileMinimum = value;
                    }
                    if (value > tileMaximum) {
                        tileMaximum = value;
                    }
                    delta = value - tileMean;
                    tileMean += delta / tileSampleCount;
                    tileMeanSqr += delta * (value - tileMean);

                    tmpValueSum += value;
                    final double value2 = value * value;
                    tmpSqrSum += value2;
                    tmpPower4Sum += value2*value2;
                    }
                }
                dataPixelOffset += dataPixelStride;
                maskPixelOffset += maskPixelStride;
            }
            dataLineOffset += dataLineStride;
            maskLineOffset += maskLineStride;
        }

        this.minimum = tileMinimum;
        this.maximum = tileMaximum;
        this.sampleCount = tileSampleCount;
        this.mean = tileMean;
        this.meanSqr = tileMeanSqr;

        this.valueSum = tmpValueSum;
        this.sqrSum = tmpSqrSum;
        this.power4Sum = tmpPower4Sum;
    }
}
