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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import org.apache.commons.math3.util.FastMath;

import javax.media.jai.Histogram;

/**
 * Provides statistic information for a raster data node at a given image resolution level.
 * Instances of the <code>Stx</code> class are created using the {@link StxFactory}.
 * <p>
 * <i>Important note: This class has been revised in BEAM 4.10. All behaviour has been moved to {@link StxFactory}
 * leaving behind this class as a pure data container. Statistics are now furthermore derived upon
 * geo-physically interpreted image data (before it operated on the raw, unscaled data). Thus, it is
 * not required to scale the returned statistical properties, e.g. we used to write
 * {@code band.scale(stx.getMean)}. This is not required anymore.</i>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @since BEAM 4.2, full revision in 4.10
 */
public class Stx {

    public static final int DEFAULT_BIN_COUNT = 512;
    public static final Scaling LOG10_SCALING = new Log10Scaling();

    private final long sampleCount;
    private final double minimum;
    private final double maximum;
    private final double mean;
    private final double standardDeviation;
    private final double median;
    private final int resolutionLevel;
    private final boolean logHistogram;
    private final boolean intHistogram;
    private final Histogram histogram;

    private final Scaling histogramScaling;

    private final double coefficientOfVariation;
    private final double enl;

    /**
     * Constructor. Avoid using it directly. instead, use the {@link StxFactory} since the constructor may change in the future.
     *
     * @param minimum           the minimum value, if it is {@link Double#NaN} the minimum is taken from the {@code histogram}
     * @param maximum           the maximum value, if it is {@link Double#NaN} the maximum is taken from the {@code histogram}
     * @param mean              the mean value, if it is {@link Double#NaN} the mean is taken from the {@code histogram}
     * @param standardDeviation the value of the standard deviation, if it is {@link Double#NaN} it is taken from the {@code histogram}
     * @param coeffOfVariation  the Coefficient of Variation
     * @param enl               Equivalent number of looks
     * @param logHistogram      {@code true} if the histogram has been computed on logarithms, see {@link #getHistogram()}
     * @param intHistogram      {@code true} if the histogram has been computed from integer samples, see {@link #getHistogram()}
     * @param histogram         the histogram
     * @param resolutionLevel   the resolution level this {@code Stx} is for
     */
    public Stx(double minimum, double maximum, double mean, double standardDeviation,
               double coeffOfVariation, double enl,
               boolean logHistogram, boolean intHistogram, Histogram histogram, int resolutionLevel) {

        Assert.argument(!Double.isNaN(minimum), "minimum must not be NaN");
        Assert.argument(!Double.isInfinite(minimum), "minimum must not be infinity");
        Assert.argument(!Double.isNaN(maximum), "maximum must not be NaN");
        Assert.argument(!Double.isInfinite(maximum), "minimum must not be infinity");
        Assert.argument(resolutionLevel >= 0, "resolutionLevel");

        // todo - this is still a lot of behaviour, move all computations to StxFactory (nf)
        this.sampleCount = StxFactory.computeSum(histogram.getBins(0));
        this.minimum = minimum;
        this.maximum = maximum;
        this.histogramScaling = getHistogramScaling(logHistogram);
        if (minimum == maximum) {
            this.mean = minimum;
            this.standardDeviation = 0.0;
            this.median = maximum;
        } else {
            this.mean = Double.isNaN(mean) ? histogramScaling.scaleInverse(histogram.getMean()[0]) : mean;
            this.standardDeviation = Double.isNaN(standardDeviation) ? histogramScaling.scaleInverse(histogram.getStandardDeviation()[0]) : standardDeviation;
            this.median = histogramScaling.scaleInverse(StxFactory.computeMedian(histogram, this.sampleCount));
        }
        this.logHistogram = logHistogram;
        this.intHistogram = intHistogram;
        this.histogram = histogram;
        this.resolutionLevel = resolutionLevel;
        this.coefficientOfVariation = coeffOfVariation;
        this.enl = enl;
    }

    /**
     * @return The minimum value.
     */
    public double getMinimum() {
        return minimum;
    }

    /**
     * @return The maximum value.
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * @return The mean value.
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return The median value (estimation based on Gaussian distribution).
     */
    public double getMedian() {
        return median;
    }

    /**
     * @return The standard deviation value.
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getCoefficientOfVariation() {
        return coefficientOfVariation;
    }

    public double getEquivalentNumberOfLooks() {
        return enl;
    }

    /**
     * Gets the histogram computed from image samples.
     * <p>
     * The returned histogram may have been computed on the logarithms of image samples.
     * In this case {@link #isLogHistogram()} returns true and it is expected that the histogram has been
     * computed from logarithms (base 10) of image samples.
     * Therefore, any statistical property retrieved from the returned histogram object such as low value, high value, bin low value,
     * mean, moment, entropy, etc. must be raised to the power of 10. Scaling is best done using the {@link #getHistogramScaling()} object.
     * <p>
     * The returned histogram may furthermore be computed from integer image data.
     * In this case {@link #isIntHistogram()} returns true and the high value of the histogram is by one higher than
     * the value returned by {@link #getMinimum()}.
     * <p>
     * The {@code numBands} property of the histogram will always be 1.
     *
     * @return The histogram.
     * @see #isIntHistogram()
     * @see #isLogHistogram()
     * @see #getHistogramScaling()
     */
    public Histogram getHistogram() {
        return histogram;
    }

    /**
     * @return {@code true} if the histogram is computed from integer samples.
     * @see #getHistogram()
     */
    public boolean isIntHistogram() {
        return intHistogram;
    }

    /**
     * @return {@code true} if the histogram is computed from log-samples.
     * @see #getHistogram()
     * @see #getHistogramScaling()
     */
    public boolean isLogHistogram() {
        return logHistogram;
    }

    /**
     * Gets the (inclusive) minimum value of the histogram bin given by the bin index.
     * <p>
     * The value returned is in units of the image samples,
     * {@link #getHistogramScaling() histogram scaling} is already applied
     *
     * @param binIndex The bin index.
     * @return The (inclusive) minimum value of the bin given by the bin index.
     */
    public double getHistogramBinMinimum(int binIndex) {
        double value = histogram.getBinLowValue(0, binIndex);
        return histogramScaling.scaleInverse(value);
    }

    /**
     * Gets the (exclusive) maximum value of the histogram bin given by the bin index.
     * <p>
     * The value returned is in units of the image samples,
     * {@link #getHistogramScaling() histogram scaling} is already applied
     *
     * @param binIndex The bin index.
     * @return The (exclusive) maximum value of the bin given by the bin index.
     */
    public double getHistogramBinMaximum(int binIndex) {
        double value = binIndex < histogram.getNumBins(0) ? histogram.getBinLowValue(0, binIndex + 1) : histogram.getHighValue(0);
        return histogramScaling.scaleInverse(value);
    }

    /**
     * Gets the width of any histogram bin.
     * <p>
     * The method's return value is undefined if {@link #isLogHistogram()} returns {@code true}. In this case you will have to use
     * {@link #getHistogramBinWidth(int)}.
     *
     * @return The width of any histogram bin.
     */
    public double getHistogramBinWidth() {
        return (getMaximum() - getMinimum()) / getHistogramBinCount();
    }

    /**
     * Gets the width of the histogram bin given by the bin index.
     * <p>
     * The value returned is in units of the image samples,
     * {@link #getHistogramScaling() histogram scaling} is already applied
     *
     * @param binIndex The bin index.
     * @return The width of the bin given by the bin index.
     */
    public double getHistogramBinWidth(int binIndex) {
        return getHistogramBinMaximum(binIndex) - getHistogramBinMinimum(binIndex);
    }

    /**
     * @return The histogram bins (sample counts).
     */
    public int[] getHistogramBins() {
        return histogram.getBins(0);
    }

    /**
     * @return The number of bins.
     */
    public int getHistogramBinCount() {
        return histogram.getNumBins(0);
    }

    /**
     * @return The image sample scaling used for deriving the histogram.
     */
    public Scaling getHistogramScaling() {
        return histogramScaling;
    }

    /**
     * @return The total number of samples seen.
     */
    public long getSampleCount() {
        return sampleCount;
    }

    /**
     * @return The image resolution level.
     */
    public int getResolutionLevel() {
        return resolutionLevel;
    }

    static Scaling getHistogramScaling(boolean logHistogram) {
        return logHistogram ? LOG10_SCALING : Scaling.IDENTITY;
    }

    /**
     * Used for histogram scaling only.
     */
    static final class Log10Scaling implements Scaling {

        private static final double LN10 = Math.log(10.0);

        @Override
        public double scale(double value) {
            // This is mathematical nonsense, but this way even negative and zero pixels can contribute to the log-distribution (nf)
            if (value <= 1.0E-9) {
                return -9.0;
            }
            return Math.log10(value);
        }

        @Override
        public double scaleInverse(double value) {
            // ca. 4x faster than Math.pow(10, value) and 2x faster than Math.exp(LN10 * value)
            return FastMath.exp(LN10 * value);
        }
    }
}
