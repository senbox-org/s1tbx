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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.jai.NoDataRaster;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.*;
import javax.media.jai.operator.MinDescriptor;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.concurrent.CancellationException;

/**
 * Instances of the <code>Stx</code> class provide statistics for a band.
 * Preliminary API. Use at your own risk.
 * <p/>
 * <i>Important note: This class has been revised in BEAM 4.10. It now operates on the
 * geo-physically interpreted image data (before it operated on the raw, unscaled data). Thus, it is
 * not required to scale the returned statistical properties, e.g. we used to write
 * {@code band.scale(stx.getMean())}. This is not required anymore.</i>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @since BEAM 4.2, full revision in 4.10
 */
public class Stx {

    public static final int DEFAULT_BIN_COUNT = 512;

    private final double minimum;
    private final double maximum;
    private final double stdDev;
    private final double mean;
    private final double median;
    private final long sampleCount;
    private final int resolutionLevel;
    private final boolean logHistogram;
    private final boolean intHistogram;
    private final Histogram histogram;

    /**
     * Constructor. Prefer using a {@link StxBuilder} since the constructor may change in the future.
     *
     * @param minimum           the minimum value
     * @param maximum           the maximum value
     * @param mean              the mean value, if it's {@link Double#NaN} the mean will be computed
     * @param stdDev            the value of the standard deviation, if it's {@link Double#NaN} it will be computed
     * @param logHistogram
     * @param intHistogram
     * @param sampleFrequencies the frequencies of the samples
     * @param resolutionLevel   the resolution level this {@code Stx} is for   @see Stx#Stx(double, double, double, double, boolean, javax.media.jai.Histogram, int)
     */
    public Stx(double minimum, double maximum, double mean, double stdDev, boolean logHistogram, boolean intHistogram, int[] sampleFrequencies,
               int resolutionLevel) {
        this(minimum, maximum, mean, stdDev,
             logHistogram, intHistogram, createHistogram(minimum, maximum + (intHistogram ? 1.0 : 0.0), sampleFrequencies),
             resolutionLevel);
    }

    /**
     * Constructor. Prefer using a {@link StxBuilder} since the constructor may change in the future.
     *
     * @param minimum         the minimum value, if it is {@link Double#NaN} the minimum is taken from the {@code histogram}
     * @param maximum         the maximum value, if it is {@link Double#NaN} the maximum is taken from the {@code histogram}
     * @param mean            the mean value, if it is {@link Double#NaN} the mean is taken from the {@code histogram}
     * @param stdDev          the value of the standard deviation, if it is {@link Double#NaN} it is taken from the {@code histogram}
     * @param logHistogram    {@code true} if the histogram has been computed on logarithms
     * @param intHistogram
     * @param histogram       the histogram
     * @param resolutionLevel the resolution level this {@code Stx} is for
     */
    Stx(double minimum, double maximum, double mean, double stdDev,
        boolean logHistogram, boolean intHistogram, Histogram histogram, int resolutionLevel) {
        this.minimum = Double.isNaN(minimum) ? histogram.getLowValue(0) : minimum;
        this.maximum = Double.isNaN(maximum) ? histogram.getHighValue(0) : maximum;
        this.mean = Double.isNaN(mean) ? histogram.getMean()[0] : mean;
        this.stdDev = Double.isNaN(stdDev) ? histogram.getStandardDeviation()[0] : stdDev;
        this.logHistogram = logHistogram;
        this.intHistogram = intHistogram;
        this.histogram = histogram;
        this.resolutionLevel = resolutionLevel;
        this.sampleCount = computeSum(histogram.getBins(0));
        this.median = computeMedian(histogram, this.sampleCount);
    }


    /**
     * Creates statistics for the given raster data node at the given resolution level.
     *
     * @param raster The raster data node.
     * @param level  The image resolution level.
     * @param pm     A progress monitor.
     * @return The statistics at the given resolution level.
     */
    public static Stx create(RasterDataNode raster, int level, ProgressMonitor pm) {
        return createImpl(raster, level, null, null, DEFAULT_BIN_COUNT, pm);
    }

    /**
     * Creates (accurate) statistics for the given raster data node.
     *
     * @param raster  The raster data node.
     * @param roiMask The mask that determines the region of interest.
     * @param pm      A progress monitor.
     * @return The (accurate) statistics.
     */
    public static Stx create(RasterDataNode raster, Mask roiMask, ProgressMonitor pm) {
        Shape maskShape = null;
        RenderedImage maskImage = null;
        if (roiMask != null) {
            maskShape = roiMask.getValidShape();
            maskImage = roiMask.getSourceImage();
        }
        return createImpl(raster, 0, maskImage, maskShape, DEFAULT_BIN_COUNT, pm);
    }

    /**
     * Creates (accurate) statistics for the given raster data node.
     *
     * @param raster   The raster data node.
     * @param roiMask  The mask that determines the region of interest.
     * @param binCount The number of bin cells used for the histogram.
     * @param pm       A progress monitor.
     * @return The (accurate) statistics.
     */
    public static Stx create(RasterDataNode raster, Mask roiMask, int binCount, ProgressMonitor pm) {
        Shape maskShape = null;
        RenderedImage maskImage = null;
        if (roiMask != null) {
            maskShape = roiMask.getValidShape();
            maskImage = roiMask.getSourceImage();
        }
        return createImpl(raster, 0, maskImage, maskShape, binCount, pm);
    }

    /**
     * Creates (accurate) statistics for the given raster data node.
     *
     * @param raster   The raster data node.
     * @param level    The image resolution level.
     * @param binCount The number of bin cells used for the histogram.
     * @param min      The minimum value.
     * @param max      The maximum value.
     * @param pm       A progress monitor.
     * @return The (accurate) statistics.
     */
    public static Stx create(RasterDataNode raster, int level, int binCount, double min, double max,
                             ProgressMonitor pm) {
        return createImpl(raster, level, null, null, binCount, min, max, pm);
    }

    /**
     * Creates (accurate) statistics for the given raster data node.
     *
     * @param raster   The raster data node.
     * @param roiMask  The mask that determines the region of interest.
     * @param binCount The number of bin cells used for the histogram.
     * @param min      The minimum value.
     * @param max      The maximum value.
     * @param pm       A progress monitor.
     * @return The (accurate) statistics.
     */
    public static Stx create(RasterDataNode raster, Mask roiMask, int binCount, double min, double max,
                             ProgressMonitor pm) {
        Shape maskShape = null;
        RenderedImage maskImage = null;
        if (roiMask != null) {
            maskShape = roiMask.getValidShape();
            maskImage = roiMask.getSourceImage();
        }
        return createImpl(raster, 0, maskImage, maskShape, binCount, min, max, pm);
    }

    // used in tests only

    /**
     * Creates statistics for the given raster data node.
     *
     * @param raster   The raster data node.
     * @param roiImage The mask image that determines the region of interest. Must be of type {@code DataBuffer.TYPE_BYTE}.
     * @param pm       A progress monitor.
     * @return The statistics.
     */
    static Stx create(RasterDataNode raster, RenderedImage roiImage, ProgressMonitor pm) {
        return createImpl(raster, 0, roiImage, null, DEFAULT_BIN_COUNT, pm);
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
        return stdDev;
    }

    /**
     * @return The histogram.
     */
    public Histogram getHistogram() {
        return histogram;
    }

    /**
     * @return {@code true} if the histogram is computed from integer samples.
     */
    public boolean isIntHistogram() {
        return intHistogram;
    }

    /**
     * @return {@code true} if the histogram is computed from log-samples.
     */
    public boolean isLogHistogram() {
        return logHistogram;
    }

    /**
     * @param binIndex The bin index.
     * @return The minimum value of the bin given by the bin index.
     */
    public double getHistogramBinMinimum(int binIndex) {
        return getMinimum() + binIndex * getHistogramBinWidth();
    }

    /**
     * @param binIndex The bin index.
     * @return The maximum value of the bin given by the bin index.
     */
    public double getHistogramBinMaximum(int binIndex) {
        return getHistogramBinMinimum(binIndex) + getHistogramBinWidth();
    }

    /**
     * @return The width of a histogram bin.
     */
    public double getHistogramBinWidth() {
        return (getMaximum() - getMinimum()) / getHistogramBinCount();
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

    static Histogram createHistogram(double minSample, double maxSample, int[] sampleFrequencies) {
        final Histogram histogram = createHistogram(sampleFrequencies.length, minSample, maxSample);
        System.arraycopy(sampleFrequencies, 0, histogram.getBins(0), 0, sampleFrequencies.length);
        return histogram;
    }

    static long computeSum(int[] sampleFrequencies) {
        long sum = 0;
        for (int sampleFrequency : sampleFrequencies) {
            sum += sampleFrequency;
        }
        return sum;
    }

    static double computeMedian(Histogram histogram, long sampleCount) {
        boolean isEven = sampleCount % 2 == 0;
        double halfSampleCount = sampleCount / 2.0;
        final int bandIndex = 0;
        int[] bins = histogram.getBins(bandIndex);
        long currentSampleCount = 0;
        int lastConsideredBinIndex = 0;
        for (int i = 0, binsLength = bins.length; i < binsLength; i++) {
            currentSampleCount += bins[i];

            if (currentSampleCount > halfSampleCount) {
                if (isEven) {
                    double binValue = getMeanOfBin(histogram, bandIndex, i);
                    double lastBinValue = getMeanOfBin(histogram, bandIndex, lastConsideredBinIndex);
                    return (lastBinValue + binValue) / 2;
                } else {
                    final double binLowValue = histogram.getBinLowValue(bandIndex, i);
                    final double binMaxValue = histogram.getBinLowValue(bandIndex, i + 1);
                    final double previousSampleCount = currentSampleCount - bins[i];
                    double weight = (halfSampleCount - previousSampleCount) / (currentSampleCount - previousSampleCount);
                    return binLowValue * (1 - weight) + binMaxValue * weight;
                }
            }
            if (bins[i] > 0) {
                lastConsideredBinIndex = i;
            }
        }
        return Double.NaN;
    }

    static double getMeanOfBin(Histogram histogram, int bandIndex, int binIndex) {
        final double binLowValue = histogram.getBinLowValue(bandIndex, binIndex);
        final double binMaxValue = histogram.getBinLowValue(bandIndex, binIndex + 1);
        return (binLowValue + binMaxValue) / 2;
    }


    static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                          int binCount, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 3);
            final SummaryStxOp summaryOp = new SummaryStxOp();
            accumulate(raster, level, maskImage, maskShape, summaryOp, SubProgressMonitor.create(pm, 1));

            double min = summaryOp.getMinimum();
            double max = summaryOp.getMaximum();
            double mean = summaryOp.getMean();
            double stdDev = summaryOp.getStandardDeviation();

            if (min == Double.MAX_VALUE && max == Double.MIN_VALUE) {
                final Histogram histogram = createHistogram(1, 0, 1);
                histogram.getBins(0)[0] = 0;
                return new Stx(0.0, 1.0, Double.NaN, Double.NaN, false, isIntHistogram(raster), histogram, level);
            }

            double off = getHighValueOffset(raster);
            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max + off, false);
            accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 1));

            // Create JAI histogram, but use our "BEAM" bins
            final Histogram histogram = createHistogram(binCount, min, max + off);
            System.arraycopy(histogramOp.getBins(), 0, histogram.getBins(0), 0, binCount);

            return createImpl(raster, level, maskImage, maskShape, histogram, min, max, mean, stdDev,
                              SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                          int binCount, double min, double max, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 3);

            double off = getHighValueOffset(raster);
            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max + off, false);
            accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 1));

            // Create JAI histogram, but use our "BEAM" bins
            final Histogram histogram = createHistogram(binCount, min, max + off);
            System.arraycopy(histogramOp.getBins(), 0, histogram.getBins(0), 0, binCount);

            return createImpl(raster, level, maskImage, maskShape, histogram, min, max, Double.NaN, Double.NaN,
                              SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                          Histogram histogram, double min, double max, double mean, double stdDev,
                          ProgressMonitor pm) {
        if (Double.isNaN(mean) || Double.isNaN(stdDev)) {
            final SummaryStxOp meanOp = new SummaryStxOp();
            accumulate(raster, level, maskImage, maskShape, meanOp, pm);
            mean = meanOp.getMean();
            stdDev = meanOp.getStandardDeviation();
        }
        return new Stx(min, max, mean, stdDev, false, isIntHistogram(raster), histogram, level);
    }

    static double getHighValueOffset(RasterDataNode raster) {
        return isIntHistogram(raster) ? 1.0 : 0.0;
    }

    static boolean isIntHistogram(RasterDataNode raster) {
        return !ProductData.isFloatingPointType(raster.getGeophysicalDataType());
    }

    static Histogram createHistogram(int binCount, double min, double max) {
        return min < max ? new Histogram(binCount, min, max, 1) : new Histogram(binCount, min, min + 1e-10, 1);
    }

    static void accumulate(RasterDataNode rasterDataNode,
                           int level,
                           RenderedImage roiImage, Shape roiShape,
                           StxOp op,
                           ProgressMonitor pm) {

        Assert.notNull(rasterDataNode, "raster");
        Assert.argument(level >= 0, "level");
        Assert.argument(roiImage == null || level == 0, "level");
        Assert.notNull(pm, "pm");

        final PlanarImage dataImage = ImageManager.getInstance().getGeophysicalImage(rasterDataNode, level);
        if (dataImage.getSampleModel().getNumBands() != 1) {
            throw new IllegalStateException("dataImage.sampleModel.numBands != 1");
        }
        PlanarImage maskImage = getEffectiveMaskImage(rasterDataNode, level, roiImage);
        Shape maskShape = getEffectiveShape(rasterDataNode, roiShape);

        accumulate(op, dataImage, maskImage, maskShape, pm);
    }

    static void accumulate(StxOp op, PlanarImage dataImage, PlanarImage maskImage, Shape maskShape, ProgressMonitor pm) {
        if (maskImage != null) {
            ensureImageCompatibility(dataImage, maskImage);
        }

        final PixelAccessor dataAccessor = new PixelAccessor(dataImage.getSampleModel(), null);
        final PixelAccessor maskAccessor = maskImage != null ? new PixelAccessor(maskImage.getSampleModel(), null) : null;

        final int tileX2 = dataImage.getTileGridXOffset() + dataImage.getNumXTiles() - 1;
        final int tileY2 = dataImage.getTileGridYOffset() + dataImage.getNumYTiles() - 1;

        try {
            pm.beginTask("Computing " + op.getName(), dataImage.getNumXTiles() * dataImage.getNumYTiles());


            for (int tileY = dataImage.getTileGridYOffset(); tileY <= tileY2; tileY++) {
                for (int tileX = dataImage.getTileGridXOffset(); tileX <= tileX2; tileX++) {
                    if (pm.isCanceled()) {
                        throw new CancellationException("Process terminated by user."); /*I18N*/
                    }
                    boolean tileContainsData = true;
                    if (maskShape != null) {
                        Rectangle dataRect = dataImage.getTileRect(tileX, tileY);
                        if (!maskShape.intersects(dataRect)) {
                            tileContainsData = false;
                        }
                    }
                    if (tileContainsData) {
                        accumulateTile(op, dataImage, maskImage, dataAccessor, maskAccessor, tileX, tileY);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    static void accumulateTile(StxOp op, PlanarImage dataImage, PlanarImage maskImage, PixelAccessor dataAccessor, PixelAccessor maskAccessor, int tileX, int tileY) {
        final Raster dataTile = dataImage.getTile(tileX, tileY);
        if (!(dataTile instanceof NoDataRaster)) {
            // data and mask image might not have the same tile size
            // --> we can not use the tile index of the one for the other, so we use the bounds
            final Raster maskTile = maskImage != null ? maskImage.getData(dataTile.getBounds()) : null;
            final Rectangle rect = new Rectangle(dataImage.getMinX(), dataImage.getMinY(),
                                                 dataImage.getWidth(), dataImage.getHeight()).intersection(
                    dataTile.getBounds());
            final UnpackedImageData dataPixels = dataAccessor.getPixels(dataTile, rect, dataImage.getSampleModel().getDataType(), false);
            final UnpackedImageData maskPixels = maskAccessor != null ? maskAccessor.getPixels(maskTile, rect, DataBuffer.TYPE_BYTE, false) : null;
            op.accumulateData(dataPixels, maskPixels);
        }
    }

    static void ensureImageCompatibility(PlanarImage dataImage, PlanarImage maskImage) {
        if (maskImage.getSampleModel().getNumBands() != 1) {
            throw new IllegalStateException("maskSampleModel.numBands != 1");
        }
        if (maskImage.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalStateException("maskSampleModel.dataType != TYPE_BYTE");
        }
        if (maskImage.getMinX() != dataImage.getMinX()) {
            throw new IllegalStateException("maskImage.getMinX() != dataImage.getMinX()");
        }
        if (maskImage.getMinY() != dataImage.getMinY()) {
            throw new IllegalStateException("maskImage.getMinY() != dataImage.getMinY()");
        }
        if (maskImage.getWidth() != dataImage.getWidth()) {
            throw new IllegalStateException("maskImage.getWidth() != dataImage.getWidth()");
        }
        if (maskImage.getHeight() != dataImage.getHeight()) {
            throw new IllegalStateException("maskImage.getWidth() != dataImage.getWidth()");
        }
        if (maskImage.getTileGridXOffset() != dataImage.getTileGridXOffset()) {
            throw new IllegalStateException("maskImage.tileGridXOffset != dataImage.tileGridXOffset");
        }
        if (maskImage.getTileGridXOffset() != dataImage.getTileGridYOffset()) {
            throw new IllegalStateException("maskImage.tileGridYOffset != dataImage.tileGridYOffset");
        }
        if (maskImage.getNumXTiles() != dataImage.getNumXTiles()) {
            throw new IllegalStateException("maskImage.numXTiles != dataImage.numXTiles");
        }
        if (maskImage.getNumYTiles() != dataImage.getNumYTiles()) {
            throw new IllegalStateException("maskImage.numYTiles != dataImage.numYTiles");
        }
    }

    static PlanarImage getEffectiveMaskImage(RasterDataNode raster, int level, RenderedImage roiImage) {
        PlanarImage maskImage = ImageManager.getInstance().getValidMaskImage(raster, level);
        if (roiImage != null) {
            if (maskImage != null) {
                final ImageLayout imageLayout = new ImageLayout();
                imageLayout.setTileWidth(maskImage.getTileWidth());
                imageLayout.setTileHeight(maskImage.getTileHeight());
                final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                maskImage = MinDescriptor.create(maskImage, roiImage, hints);
            } else {
                maskImage = PlanarImage.wrapRenderedImage(roiImage);
            }
        }
        return maskImage;
    }

    static Shape getEffectiveShape(RasterDataNode raster, Shape maskShape) {
        Shape validShape = raster.getValidShape();
        Shape effectiveShape = validShape;
        if (validShape != null && maskShape != null) {
            Area area = new Area(validShape);
            area.intersect(new Area(maskShape));
            effectiveShape = area;
        } else if (maskShape != null) {
            effectiveShape = maskShape;
        }
        return effectiveShape;
    }

    @Deprecated
    public double getHistogramBinMin(int binIndex) {
        return getHistogramBinMinimum(binIndex);
    }

    @Deprecated
    public double getHistogramBinMax(int binIndex) {
        return getHistogramBinMaximum(binIndex);
    }

}
