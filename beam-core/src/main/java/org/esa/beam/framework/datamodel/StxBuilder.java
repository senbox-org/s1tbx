package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

import javax.media.jai.Histogram;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * Builder for {@link Stx} instances (builder pattern).
 *
 * @author Norman Fomferra
 */
public class StxBuilder {
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

    public StxBuilder() {
        this(null);
    }

    public StxBuilder(RasterDataNode raster) {
        this.raster = raster;
    }

    public StxBuilder withMinimum(Number minimum) {
        this.minimum = minimum;
        return this;
    }

    public StxBuilder withMaximum(Number maximum) {
        this.maximum = maximum;
        return this;
    }

    public StxBuilder withMean(Number mean) {
        this.mean = mean;
        return this;
    }

    public StxBuilder withStdDev(Number stdDev) {
        this.stdDev = stdDev;
        return this;
    }

    public StxBuilder withHistogram(Histogram histogram) {
        this.histogram = histogram;
        return this;
    }

    public StxBuilder withResolutionLevel(Integer resolutionLevel) {
        this.resolutionLevel = resolutionLevel;
        return this;
    }

    public StxBuilder withRoiMask(Mask roiMask) {
        this.roiMask = roiMask;
        return this;
    }

    public StxBuilder withRoiImage(RenderedImage roiImage) {
        this.roiImage = roiImage;
        return this;
    }

    public StxBuilder withHistogramBinCount(Integer histogramBinCount) {
        this.histogramBinCount = histogramBinCount;
        return this;
    }

    public StxBuilder withHistogramBins(int[] histogramBins) {
        this.histogramBins = histogramBins;
        return this;
    }

    public StxBuilder withIntHistogram(boolean intHistogram) {
        this.intHistogram = intHistogram;
        return this;
    }

    public StxBuilder withLogHistogram(boolean logHistogram) {
        this.logHistogram = logHistogram;
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
                    boolean intHistogram = raster.getGeophysicalImage().getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT;
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
            boolean intHistogram = this.intHistogram != null ? this.intHistogram : false;
            double offset = intHistogram ? 1.0 : 0.0;
            histogram = Stx.createHistogram(minimum, maximum + offset, this.histogramBins);
        } else {
            throw new IllegalStateException("Failed to derive histogram");
        }

        return new Stx(minimum, maximum, mean, stdDev, histogram, logHistogram, level);
    }
}
