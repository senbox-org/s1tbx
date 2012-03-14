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
 * The factory for {@link Stx} instances.
 * The design of this class is following the Builder pattern.
 *
 * @author Norman Fomferra
 */
public class StxFactory {
    public static final int DEFAULT_BIN_COUNT = 512;

    private Number minimum;
    private Number maximum;
    private Number mean;
    private Number stdDev;
    private Histogram histogram;
    private Integer resolutionLevel;
    private Mask roiMask;
    private RenderedImage roiImage;
    private Integer histogramBinCount;
    private Boolean intHistogram;
    private Boolean logHistogram;
    private int[] histogramBins;

    public StxFactory() {
    }

    public StxFactory withMinimum(Number minimum) {
        this.minimum = minimum;
        return this;
    }

    public StxFactory withMaximum(Number maximum) {
        this.maximum = maximum;
        return this;
    }

    public StxFactory withMean(Number mean) {
        this.mean = mean;
        return this;
    }

    public StxFactory withStdDev(Number stdDev) {
        this.stdDev = stdDev;
        return this;
    }

    public StxFactory withIntHistogram(boolean intHistogram) {
        this.intHistogram = intHistogram;
        return this;
    }

    public StxFactory withLogHistogram(boolean logHistogram) {
        this.logHistogram = logHistogram;
        return this;
    }

    public StxFactory withHistogram(Histogram histogram) {
        this.histogram = histogram;
        return this;
    }

    public StxFactory withResolutionLevel(Integer resolutionLevel) {
        this.resolutionLevel = resolutionLevel;
        return this;
    }

    public StxFactory withRoiMask(Mask roiMask) {
        this.roiMask = roiMask;
        return this;
    }

    public StxFactory withRoiImage(RenderedImage roiImage) {
        this.roiImage = roiImage;
        return this;
    }

    public StxFactory withHistogramBinCount(Integer histogramBinCount) {
        this.histogramBinCount = histogramBinCount;
        return this;
    }

    public StxFactory withHistogramBins(int[] histogramBins) {
        this.histogramBins = histogramBins;
        return this;
    }

    /**
     * Creates an {@code Stx} instance.
     *
     * @return The statistics.
     */
    public Stx create() {
        return create(null, ProgressMonitor.NULL);
    }

    /**
     * Computes statistics for the given raster data node.
     *
     * @param raster The raster data node.
     * @param pm     A progress monitor.
     * @return The statistics.
     */
    public Stx create(RasterDataNode raster, ProgressMonitor pm) {

        double minimum = this.minimum != null ? this.minimum.doubleValue() : Double.NaN;
        double maximum = this.maximum != null ? this.maximum.doubleValue() : Double.NaN;
        double mean = this.mean != null ? this.mean.doubleValue() : Double.NaN;
        double stdDev = this.stdDev != null ? this.stdDev.doubleValue() : Double.NaN;
        boolean logHistogram = this.logHistogram != null ? this.logHistogram : false;
        boolean intHistogram = this.intHistogram != null ? this.intHistogram : false;
        int level = this.resolutionLevel != null ? this.resolutionLevel : 0;

        Histogram histogram = this.histogram;

        if (raster != null) {

            Shape maskShape = null;
            RenderedImage maskImage = null;
            if (roiMask != null) {
                maskShape = roiMask.getValidShape();
                maskImage = roiMask.getSourceImage();
            }

            boolean mustComputeSummaryStx = this.minimum == null || this.maximum == null;
            boolean mustComputeHistogramStx = this.histogram == null && this.histogramBins == null;

            try {
                pm.beginTask("Computing statistics", mustComputeSummaryStx && mustComputeHistogramStx ? 100 : 50);

                if (mustComputeSummaryStx) {
                    final SummaryStxOp meanOp = new SummaryStxOp();
                    accumulate(raster, level, maskImage, maskShape, meanOp, SubProgressMonitor.create(pm, 50));
                    if (this.minimum == null) {
                        minimum = meanOp.getMinimum();
                    }
                    if (this.maximum == null) {
                        maximum = meanOp.getMaximum();
                    }
                    if (this.mean == null) {
                        mean = meanOp.getMean();
                    }
                    if (this.stdDev == null) {
                        stdDev = meanOp.getStandardDeviation();
                    }
                }

                if (mustComputeHistogramStx) {
                    int binCount = histogramBinCount != null ? histogramBinCount : DEFAULT_BIN_COUNT;
                    intHistogram = raster.getGeophysicalImage().getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT;
                    final HistogramStxOp histogramOp = new HistogramStxOp(binCount, minimum, maximum, intHistogram, logHistogram);
                    accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 50));
                    histogram = histogramOp.getHistogram();
                }

            } finally {
                pm.done();
            }
        }

        if (histogram == null) {
            if (this.histogramBins != null) {
                histogram = createHistogram(minimum, maximum, logHistogram, intHistogram, this.histogramBins);
            } else {
                throw new IllegalStateException("Failed to derive histogram");
            }
        }

        if (Double.isNaN(minimum)) {
            if (!logHistogram) {
                minimum = histogram.getLowValue(0);
            } else {
                throw new IllegalStateException("Failed to derive minimum");
            }
        }

        if (Double.isNaN(maximum)) {
            if (!logHistogram) {
                maximum = histogram.getHighValue(0);
            } else {
                throw new IllegalStateException("Failed to derive maximum");
            }
        }

        return new Stx(minimum, maximum, mean, stdDev, logHistogram, intHistogram, histogram, level);
    }

    static void accumulate(RasterDataNode rasterDataNode,
                           int level,
                           RenderedImage roiImage,
                           Shape roiShape,
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

    static void accumulateTile(StxOp op,
                               PlanarImage dataImage,
                               PlanarImage maskImage,
                               PixelAccessor dataAccessor,
                               PixelAccessor maskAccessor,
                               int tileX, int tileY) {
        final Raster dataTile = dataImage.getTile(tileX, tileY);
        if (!(dataTile instanceof NoDataRaster)) {
            // data and mask image might not have the same tile size
            // --> we can not use the tile index of the one for the other, so we use the bounds
            final Raster maskTile = maskImage != null ? maskImage.getData(dataTile.getBounds()) : null;
            final Rectangle rect = new Rectangle(dataImage.getMinX(), dataImage.getMinY(),
                                                 dataImage.getWidth(), dataImage.getHeight())
                    .intersection(dataTile.getBounds());
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

    static boolean isIntHistogram(RasterDataNode raster) {
        return !ProductData.isFloatingPointType(raster.getGeophysicalDataType());
    }

    static Histogram createHistogram(int binCount, double minimum, double maximum, boolean logHistogram, boolean intHistogram) {
        Scaling histogramScaling = Stx.getHistogramScaling(logHistogram, minimum);
        double adjustedMaximum = maximum;
        if (intHistogram) {
            adjustedMaximum = maximum + 1.0;
        } else if (minimum == maximum) {
            adjustedMaximum = minimum + 1e-10;
        }
        return new Histogram(binCount,
                             histogramScaling.scale(minimum),
                             histogramScaling.scale(adjustedMaximum),
                             1);
    }

    static Histogram createHistogram(double minimum, double maximum, boolean logHistogram, boolean intHistogram, int[] bins) {
        final Histogram histogram = createHistogram(bins.length, minimum, maximum, logHistogram, intHistogram);
        System.arraycopy(bins, 0, histogram.getBins(0), 0, bins.length);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO - check usages of following methods and, if required, replace by StxFactory. (nf)

    @Deprecated
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

            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max, isIntHistogram(raster), false);
            accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 1));
            final Histogram histogram = histogramOp.getHistogram();

            return createImpl(raster, level, maskImage, maskShape, histogram, min, max, mean, stdDev,
                              SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    @Deprecated
    static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
                          int binCount, double min, double max, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 3);

            final HistogramStxOp histogramOp = new HistogramStxOp(binCount, min, max, isIntHistogram(raster), false);
            accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 1));
            final Histogram histogram = histogramOp.getHistogram();

            return createImpl(raster, level, maskImage, maskShape, histogram, min, max, Double.NaN, Double.NaN,
                              SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    @Deprecated
    private static Stx createImpl(RasterDataNode raster, int level, RenderedImage maskImage, Shape maskShape,
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


}
