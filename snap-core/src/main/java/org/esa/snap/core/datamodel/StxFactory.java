package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.jai.NoDataRaster;
import org.esa.snap.core.image.ImageManager;

import javax.media.jai.Histogram;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.operator.MinDescriptor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
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
    private Number standardDeviation;
    private Histogram histogram;
    private Integer resolutionLevel;
    private Mask roiMask;
    private RenderedImage roiImage;
    private Shape roiShape;
    private Integer histogramBinCount;
    private Boolean intHistogram;
    private Boolean logHistogram;
    private int[] histogramBins;

    private Number coefficientOfVariation;
    private Number enl;

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

    public StxFactory withStandardDeviation(Number standardDeviation) {
        this.standardDeviation = standardDeviation;
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

    /**
     * @param roiImage The ROI image. Ignored if ROI mask is used.
     * @return This instance.
     */
    public StxFactory withRoiImage(RenderedImage roiImage) {
        this.roiImage = roiImage;
        return this;
    }

    public StxFactory withRoiShape(Shape roiShape) {
        this.roiShape = roiShape;
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
     * Computes statistics for the given set of raster data nodes.
     *
     * @param roiMasks An array of roi masks, must be <code>null</code> or have the same length as <code>rasters</code>.
     * @param rasters  The raster data nodes.
     * @param pm       A progress monitor.
     * @return The statistics.
     */
    public Stx create(Mask[] roiMasks, RasterDataNode[] rasters, ProgressMonitor pm) {
        double minimum = this.minimum != null ? this.minimum.doubleValue() : Double.NaN;
        double maximum = this.maximum != null ? this.maximum.doubleValue() : Double.NaN;
        double mean = this.mean != null ? this.mean.doubleValue() : Double.NaN;
        double stdDev = this.standardDeviation != null ? this.standardDeviation.doubleValue() : Double.NaN;
        boolean logHistogram = this.logHistogram != null ? this.logHistogram : false;
        boolean intHistogram = this.intHistogram != null ? this.intHistogram : false;
        int level = this.resolutionLevel != null ? this.resolutionLevel : 0;

        double coeffOfVariation = this.coefficientOfVariation != null ? this.coefficientOfVariation.doubleValue() : Double.NaN;
        double enl = this.enl != null ? this.enl.doubleValue() : Double.NaN;
        Histogram histogram = this.histogram;

        Assert.argument(roiMasks == null || roiMasks.length == rasters.length, "roiMasks == null || roiMasks.length == rasters.length");

        final List<RasterDataNode> filteredRasterList = new ArrayList<RasterDataNode>();
        for (RasterDataNode rasterDataNode : rasters) {
            if (rasterDataNode != null) {
                filteredRasterList.add(rasterDataNode);
            }
        }
        if (!filteredRasterList.isEmpty()) {
            RasterDataNode[] filteredRasters = filteredRasterList.toArray(new RasterDataNode[filteredRasterList.size()]);

            Shape[] roiShapes = new Shape[rasters.length];
            RenderedImage[] roiImages = new RenderedImage[rasters.length];

            roiImages[0] = this.roiImage;
            if (roiMasks != null) {
                roiShapes[0] = this.roiShape;
                for (int i = 0; i < roiMasks.length; i++) {
                    if (roiMasks[i] == null) {
                        roiShapes[i] = null;
                        roiImages[i] = null;
                    } else {
                        if (roiMasks[i].getValidShape() != null) {
                            roiShapes[i] = roiMasks[i].getValidShape();
                        }
                        roiImages[i] = roiMasks[i].getSourceImage();
                    }
                }
            }

            if (this.intHistogram == null) {
                intHistogram = filteredRasters[0].getGeophysicalImage().getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT;
            }

            boolean mustComputeSummaryStx = this.minimum == null || this.maximum == null;
            boolean mustComputeHistogramStx = this.histogram == null && this.histogramBins == null;

            try {
                pm.beginTask("Computing statistics", mustComputeSummaryStx && mustComputeHistogramStx ? 100 : 50);

                if (mustComputeSummaryStx) {
                    final SummaryStxOp meanOp = new SummaryStxOp();
                    for (int i = 0; i < filteredRasters.length; i++) {
                        final RasterDataNode rasterDataNode = filteredRasters[i];
                        accumulate(rasterDataNode, level, roiImages[i], roiShapes[i], meanOp, SubProgressMonitor.create(pm, 50));
                    }
                    if (this.minimum == null) {
                        minimum = meanOp.getMinimum();
                    }
                    if (this.maximum == null) {
                        maximum = meanOp.getMaximum();
                    }
                    if (this.mean == null) {
                        mean = meanOp.getMean();
                    }
                    if (this.standardDeviation == null) {
                        stdDev = meanOp.getStandardDeviation();
                    }
                    if (this.coefficientOfVariation == null) {
                        coeffOfVariation = meanOp.getCoefficientOfVariation(filteredRasters[0].getUnit());
                    }
                    if (this.enl == null) {
                        enl = meanOp.getEquivalentNumberOfLooks(filteredRasters[0].getUnit());
                    }
                }

                if (mustComputeHistogramStx) {
                    int binCount = histogramBinCount != null ? histogramBinCount : DEFAULT_BIN_COUNT;
                    final HistogramStxOp histogramOp = new HistogramStxOp(binCount, minimum, maximum, intHistogram, logHistogram);
                    for (int i = 0; i < filteredRasters.length; i++) {
                        final RasterDataNode rasterDataNode = filteredRasters[i];
                        accumulate(rasterDataNode, level, roiImages[i], roiShapes[i], histogramOp, SubProgressMonitor.create(pm, 50));
                    }
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

        return new Stx(minimum, maximum, mean, stdDev, coeffOfVariation, enl, logHistogram, intHistogram, histogram, level);
    }

    /**
     * Computes statistics for the given raster data node.
     *
     * @param raster The raster data node.
     * @param pm     A progress monitor.
     * @return The statistics.
     */
    public Stx create(RasterDataNode raster, ProgressMonitor pm) {
        if (roiMask != null) {
            return create(new Mask[]{roiMask}, new RasterDataNode[]{raster}, pm);
        }
        return create(null, new RasterDataNode[]{raster}, pm);
    }

    public static void accumulate(RasterDataNode rasterDataNode,
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

        try {
            pm.beginTask("Computing " + op.getName(), dataImage.getNumXTiles() * dataImage.getNumYTiles());

            for (int tileY = dataImage.getMinTileY(); tileY <= dataImage.getMaxTileY(); tileY++) {
                for (int tileX = dataImage.getMinTileX(); tileX <= dataImage.getMaxTileX(); tileX++) {
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
            throw new IllegalStateException("maskImage.getHeight() != dataImage.getHeight()");
        }
    }

    static PlanarImage getEffectiveMaskImage(RasterDataNode raster, int level, RenderedImage roiImage) {
        PlanarImage maskImage = ImageManager.getInstance().getValidMaskImage(raster, level);
        if (maskImage == roiImage) {
            return maskImage;
        }
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

    static Shape getEffectiveShape(RasterDataNode raster, Shape roiShape) {
        Shape validShape = raster.getValidShape();
        if (validShape == roiShape) {
            return validShape;
        }
        Shape effectiveShape = validShape;
        if (validShape != null && roiShape != null) {
            Area area = new Area(validShape);
            area.intersect(new Area(roiShape));
            effectiveShape = area;
        } else if (roiShape != null) {
            effectiveShape = roiShape;
        }
        return effectiveShape;
    }

    static Histogram createHistogram(int binCount, double minimum, double maximum, boolean logHistogram, boolean intHistogram) {
        Scaling histogramScaling = Stx.getHistogramScaling(logHistogram);
        if (intHistogram) {
            maximum += 1.0;
        } else if (maximum == minimum) {
            if (maximum < Double.MAX_VALUE) {
                maximum = Math.nextUp(maximum);
            } else {
                minimum = Math.nextAfter(minimum, Double.NEGATIVE_INFINITY);
            }
        }
        return new Histogram(binCount,
                             histogramScaling.scale(minimum),
                             histogramScaling.scale(maximum),
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
}
