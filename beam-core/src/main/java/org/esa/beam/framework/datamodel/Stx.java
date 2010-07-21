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

import javax.media.jai.Histogram;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MinDescriptor;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.concurrent.CancellationException;

/**
 * Instances of the <code>Stx</code> class provide statistics for a band.
 * Preliminary API. Use at your own risk.
 *
 * @since BEAM 4.2
 */
public class Stx {

    public static final int DEFAULT_BIN_COUNT = 512;

    private final double min;
    private final double max;
    private final double stdDev;
    private final long sampleCount;
    private final int resolutionLevel;
    private final Histogram histogram;
    private final double mean;
    private final double median;

    public static Stx create(RasterDataNode raster, int level, ProgressMonitor pm) {
        return createImpl(raster, level, null, null, DEFAULT_BIN_COUNT, pm);
    }

    /**
     * @deprecated since BEAM 4.7, use {@link #create(RasterDataNode, Mask, ProgressMonitor)} instead.
     */
    @Deprecated
    public static Stx create(RasterDataNode raster, RenderedImage roiImage, ProgressMonitor pm) {
        return createImpl(raster, 0, roiImage, null, DEFAULT_BIN_COUNT, pm);
    }

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
     * @deprecated since BEAM 4.7, use {@link #create(RasterDataNode, Mask, int, ProgressMonitor)} instead.
     */
    @Deprecated
    public static Stx create(RasterDataNode raster, RenderedImage roiImage, int binCount, ProgressMonitor pm) {
        return createImpl(raster, 0, roiImage, null, binCount, pm);
    }

    public static Stx create(RasterDataNode raster, Mask roiMask, int binCount, ProgressMonitor pm) {
        Shape maskShape = null;
        RenderedImage maskImage = null;
        if (roiMask != null) {
            maskShape = roiMask.getValidShape();
            maskImage = roiMask.getSourceImage();
        }
        return createImpl(raster, 0, maskImage, maskShape, binCount, pm);
    }

    public static Stx create(RasterDataNode raster, int level, int binCount, double min, double max,
                             ProgressMonitor pm) {
        return createImpl(raster, level, null, null, binCount, min, max, pm);
    }

    /**
     * @deprecated since BEAM 4.7, use {@link #create(RasterDataNode, Mask, int, double, double, ProgressMonitor)} instead.
     */
    @Deprecated
    public static Stx create(RasterDataNode raster, RenderedImage roiImage, int binCount, double min, double max,
                             ProgressMonitor pm) {
        return createImpl(raster, 0, roiImage, null, binCount, min, max, pm);
    }

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


    /**
     * Creates a {@code Stx} object with the given Parameter.
     *
     * @param min               the minimum value
     * @param max               the maximum value
     * @param mean              the mean value, if it's {@link Double#NaN} the mean will be computed
     * @param stdDev            the value of the standard deviation, if it's {@link Double#NaN} it will be computed
     * @param intType           if true, statistics are computed from a data basis of integer number type.
     * @param sampleFrequencies the frequencies of the samples
     * @param resolutionLevel   the resolution level this {@code Stx} is for
     *
     * @see Stx#Stx(double, double, double, double, javax.media.jai.Histogram, int)
     */
    public Stx(double min, double max, double mean, double stdDev, boolean intType, int[] sampleFrequencies,
               int resolutionLevel) {
        this(min, max, mean, stdDev, createHistogram(min, max + (intType ? 1.0 : 0.0), sampleFrequencies),
             resolutionLevel);
    }

    /**
     * Creates a {@code Stx} object with the given Parameter.
     *
     * @param min             the minimum value
     * @param max             the maximum value
     * @param mean            the mean value, if it's {@link Double#NaN} the mean is taken from the {@code histogram}
     * @param stdDev          the value of the standard deviation, if it's {@link Double#NaN} it is taken from the {@code histogram}
     * @param histogram       the histogram
     * @param resolutionLevel the resolution level this {@code Stx} is for
     *
     * @see Stx#Stx(double, double, double, double, javax.media.jai.Histogram, int)
     */
    private Stx(double min, double max, double mean, double stdDev, Histogram histogram, int resolutionLevel) {
        this.min = min;
        this.max = max;
        this.mean = Double.isNaN(mean) ? histogram.getMean()[0] : mean;
        this.stdDev = Double.isNaN(stdDev) ? histogram.getStandardDeviation()[0] : stdDev;
        this.histogram = histogram;
        this.resolutionLevel = resolutionLevel;
        this.sampleCount = computeSum(histogram.getBins(0));
        this.median = computeMedian(histogram, this.sampleCount);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getStandardDeviation() {
        return stdDev;
    }

    public double getHistogramBinMin(int binIndex) {
        return getMin() + binIndex * getHistogramBinWidth();
    }

    public double getHistogramBinMax(int binIndex) {
        return getHistogramBinMin(binIndex) + getHistogramBinWidth();
    }

    public double getHistogramBinWidth() {
        return (getMax() - getMin()) / getHistogramBinCount();
    }

    public int[] getHistogramBins() {
        return histogram.getBins(0);
    }

    public int getHistogramBinCount() {
        return histogram.getNumBins(0);
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public int getResolutionLevel() {
        return resolutionLevel;
    }

    private static Histogram createHistogram(double minSample, double maxSample, int[] sampleFrequencies) {
        final Histogram histogram = createHistogram(sampleFrequencies.length, minSample, maxSample);
        System.arraycopy(sampleFrequencies, 0, histogram.getBins(0), 0, sampleFrequencies.length);
        return histogram;
    }

    private static long computeSum(int[] sampleFrequencies) {
        long sum = 0;
        for (int sampleFrequency : sampleFrequencies) {
            sum += sampleFrequency;
        }
        return sum;
    }

    private static double computeMedian(Histogram histogram, long sampleCount) {
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

    private static double getMeanOfBin(Histogram histogram, int bandIndex, int binIndex) {
        final double binLowValue = histogram.getBinLowValue(bandIndex, binIndex);
        final double binMaxValue = histogram.getBinLowValue(bandIndex, binIndex + 1);
        return (binLowValue + binMaxValue) / 2;
    }


    private static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                                  int binCount, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 3);
            final SummaryStxOp summaryOp = new SummaryStxOp();
            accumulate(raster, level, maskImage, maskShape, summaryOp, SubProgressMonitor.create(pm, 1));

            double min = summaryOp.getMinimum();
            double max = summaryOp.getMaximum();
            double mean = summaryOp.getMean();
            double stdDev = summaryOp.getStdDev();

            if (min == Double.MAX_VALUE && max == Double.MIN_VALUE) {
                final Histogram histogram = createHistogram(1, 0, 1);
                histogram.getBins(0)[0] = 0;
                return new Stx(0.0, 1.0, Double.NaN, Double.NaN, histogram, level);
            }

            double off = getHighValueOffset(raster);
            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max + off);
            accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 1));

            // Create JAI histo, but use our "BEAM" bins
            final Histogram histogram = createHistogram(binCount, min, max + off);
            System.arraycopy(histogramOp.getBins(), 0, histogram.getBins(0), 0, binCount);


            return createImpl(raster, level, maskImage, maskShape, histogram, min, max, mean, stdDev,
                              SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    private static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                                  int binCount, double min, double max, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 3);

            double off = getHighValueOffset(raster);
            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max + off);
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

    private static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                                  Histogram histogram, double min, double max, double mean, double stdDev,
                                  ProgressMonitor pm) {
        if (Double.isNaN(mean) || Double.isNaN(stdDev)) {
            final SummaryStxOp meanOp = new SummaryStxOp();
            accumulate(raster, level, maskImage, maskShape, meanOp, pm);
            mean = meanOp.getMean();
            stdDev = meanOp.getStdDev();
        }

        return new Stx(min, max, mean, stdDev, histogram, level);
    }

    private static double getHighValueOffset(RasterDataNode raster) {
        return ProductData.isIntType(raster.getDataType()) ? 1.0 : 0.0;
    }

    private static Histogram createHistogram(int binCount, double min, double max) {
        return min < max ? new Histogram(binCount, min, max, 1) : new Histogram(binCount, min, min + 1e-10, 1);
    }

    private static void accumulate(RasterDataNode raster,
                                   int level,
                                   RenderedImage roiImage, Shape maskShape,
                                   StxOp op,
                                   ProgressMonitor pm) {

        Assert.notNull(raster, "raster");
        Assert.argument(level >= 0, "level");
        Assert.argument(roiImage == null || level == 0, "level");
        Assert.notNull(pm, "pm");

        final PlanarImage dataImage = ImageManager.getInstance().getSourceImage(raster, level);
        final SampleModel dataSampleModel = dataImage.getSampleModel();
        if (dataSampleModel.getNumBands() != 1) {
            throw new IllegalStateException("dataSampleModel.numBands != 1");
        }
        final PixelAccessor dataAccessor = new PixelAccessor(dataSampleModel, null);

        RenderedImage maskImage = ImageManager.getInstance().getValidMaskImage(raster, level);
        if (roiImage != null) {
            if (maskImage != null) {
                maskImage = MinDescriptor.create(maskImage, roiImage, null);
            } else {
                maskImage = roiImage;
            }
        }
        Shape validShape = raster.getValidShape();
        Shape effectiveShape = validShape;
        if (validShape != null && maskShape != null) {
            Area area = new Area(validShape);
            area.intersect(new Area(maskShape));
            effectiveShape = area;
        } else if (maskShape != null) {
            effectiveShape = maskShape;
        }

        final PixelAccessor maskAccessor;
        if (maskImage != null) {
            SampleModel maskSampleModel = maskImage.getSampleModel();
            if (maskSampleModel.getNumBands() != 1) {
                throw new IllegalStateException("maskSampleModel.numBands != 1");
            }
            if (maskSampleModel.getDataType() != DataBuffer.TYPE_BYTE) {
                throw new IllegalStateException("maskSampleModel.dataType != TYPE_BYTE");
            }
            maskAccessor = new PixelAccessor(maskSampleModel, null);
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
        } else {
            maskAccessor = null;
        }


        final int numXTiles = dataImage.getNumXTiles();
        final int numYTiles = dataImage.getNumYTiles();

        final int tileX1 = dataImage.getTileGridXOffset();
        final int tileY1 = dataImage.getTileGridYOffset();
        final int tileX2 = tileX1 + numXTiles - 1;
        final int tileY2 = tileY1 + numYTiles - 1;

        if (maskImage != null) {
            if (maskImage.getTileGridXOffset() != tileX1) {
                throw new IllegalStateException("maskImage.getTileGridXOffset() != dataImage.getTileGridXOffset()");
            }
            if (maskImage.getTileGridXOffset() != tileY1) {
                throw new IllegalStateException("maskImage.getTileGridYOffset() != dataImage.getTileGridYOffset()");
            }
            if (maskImage.getNumXTiles() != numXTiles) {
                throw new IllegalStateException("maskImage.getNumXTiles() != dataImage.getNumXTiles()");
            }
            if (maskImage.getNumYTiles() != numYTiles) {
                throw new IllegalStateException("maskImage.getNumYTiles() != dataImage.getNumYTiles()");
            }
        }

        try {
            pm.beginTask("Computing " + op.getName(), numXTiles * numYTiles);
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                    if (pm.isCanceled()) {
                        throw new CancellationException("Process terminated by user."); /*I18N*/
                    }
                    boolean tileContainsData = true;
                    if (effectiveShape != null) {
                        Rectangle dataRect = dataImage.getTileRect(tileX, tileY);
                        if (!effectiveShape.intersects(dataRect)) {
                            tileContainsData = false;
                        }
                    }
                    if (tileContainsData) {
                        final Raster dataTile = dataImage.getTile(tileX, tileY);
                        if (!(dataTile instanceof NoDataRaster)) {
                            // data and mask image might not have the same tile size
                            // --> we can not use the tile index of the one for the other, so we use the bounds
                            final Raster maskTile = maskImage != null ? maskImage.getData(dataTile.getBounds()) : null;
                            final Rectangle r = new Rectangle(dataImage.getMinX(), dataImage.getMinY(),
                                                              dataImage.getWidth(), dataImage.getHeight()).intersection(
                                    dataTile.getBounds());
                            switch (raster.getDataType()) {
                                case ProductData.TYPE_UINT8:
                                    op.accumulateDataUByte(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_INT8:
                                    op.accumulateDataByte(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_UINT16:
                                    op.accumulateDataUShort(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_INT16:
                                    op.accumulateDataShort(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_UINT32:
                                    op.accumulateDataUInt(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_INT32:
                                    op.accumulateDataInt(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_FLOAT32:
                                    op.accumulateDataFloat(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                                case ProductData.TYPE_FLOAT64:
                                    op.accumulateDataDouble(dataAccessor, dataTile, maskAccessor, maskTile, r);
                                    break;
                            }
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }
}
