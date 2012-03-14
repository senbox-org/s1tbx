package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

import javax.media.jai.Histogram;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * The factory for {@link Stx} instances.
 * The design of this class is following the Builder pattern.
 *
 * @author Norman Fomferra
 */
public class StxFactory {
    public static final int DEFAULT_BIN_COUNT = 512;

    private final RasterDataNode raster;

    Number minimum;
    Number maximum;
    Number mean;
    Number stdDev;
    Histogram histogram;
    Integer resolutionLevel;
    Mask roiMask;
    RenderedImage roiImage;
    Integer histogramBinCount;
    Boolean intHistogram;
    Boolean logHistogram;
    int[] histogramBins;

    public StxFactory() {
        this(null);
    }

    public StxFactory(RasterDataNode raster) {
        this.raster = raster;
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

    public Stx create() {
        return create(ProgressMonitor.NULL);
    }

    public Stx create(ProgressMonitor pm) {

        double minimum = this.minimum != null ? this.minimum.doubleValue() : Double.NaN;
        double maximum = this.maximum != null ? this.maximum.doubleValue() : Double.NaN;
        double mean = this.mean != null ? this.mean.doubleValue() : Double.NaN;
        double stdDev = this.stdDev != null ? this.stdDev.doubleValue() : Double.NaN;
        int level = this.resolutionLevel != null ? this.resolutionLevel : 0;
        boolean logHistogram = this.logHistogram != null ? this.logHistogram : false;
        boolean intHistogram = this.intHistogram != null ? this.intHistogram : false;

        Histogram histogram;

        if (raster != null) {

            Shape maskShape = null;
            RenderedImage maskImage = null;
            if (roiMask != null) {
                maskShape = roiMask.getValidShape();
                maskImage = roiMask.getSourceImage();
            }

            boolean mustComputeSummaryStx = this.minimum == null || this.maximum == null || this.mean == null || this.stdDev == null;
            boolean mustComputeHistogramStx = this.histogram == null && this.histogramBins == null;

            try {
                pm.beginTask("Computing statistics", mustComputeSummaryStx && mustComputeHistogramStx ? 100 : 50);

                if (mustComputeSummaryStx) {
                    final SummaryStxOp meanOp = new SummaryStxOp();
                    Stx.accumulate(raster, level, maskImage, maskShape, meanOp, SubProgressMonitor.create(pm, 50));
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
                    double offset = intHistogram ? 1.0 : 0.0;
                    final HistogramStxOp histogramOp = new HistogramStxOp(binCount, minimum, maximum + offset, logHistogram);
                    Stx.accumulate(raster, level, maskImage, maskShape, histogramOp, SubProgressMonitor.create(pm, 50));
                    histogram = Stx.createHistogram(binCount, minimum, maximum + offset);
                    System.arraycopy(histogramOp.getBins(), 0, histogram.getBins(0), 0, binCount);
                }

            } finally {
                pm.done();
            }
        }

        if (this.histogram == null && this.histogramBins != null) {
            if (Double.isNaN(minimum)) {
                throw new IllegalStateException("Failed to derive minimum");
            }
            if (Double.isNaN(maximum)) {
                throw new IllegalStateException("Failed to derive maximum");
            }
            double offset = intHistogram ? 1.0 : 0.0;
            histogram = Stx.createHistogram(minimum, maximum + offset, this.histogramBins);
        } else {
            throw new IllegalStateException("Failed to derive histogram");
        }

        return new Stx(minimum, maximum, mean, stdDev, logHistogram, intHistogram, histogram, level);
    }
}
