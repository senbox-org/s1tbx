package org.esa.beam.framework.datamodel;

import org.esa.beam.jai.ImageManager;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.concurrent.CancellationException;

import javax.media.jai.Histogram;
import javax.media.jai.PixelAccessor;
import javax.media.jai.ROI;
import javax.media.jai.UnpackedImageData;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

/**
 * Instances of the <code>Stx</code> class provide statistics for a band.
 * Premininary API. Use at your own risk.
 *
 * @since BEAM 4.2
 */
public class Stx {
    public static final int DEFAULT_BIN_COUNT = 512;

    private final double min;
    private final double max;
    private final long sampleCount;
    private final int resolutionLevel;
    private final Histogram histogram;

    public static Stx create(RasterDataNode raster, int level, ProgressMonitor pm) {
        return create(raster, level, null, DEFAULT_BIN_COUNT, pm);
    }

    public static Stx create(RasterDataNode raster, int level, int binCount, double min, double max, ProgressMonitor pm) {
        return create(raster, level, null, binCount, min, max, pm);
    }

    public static Stx create(RasterDataNode raster, ROI roi, ProgressMonitor pm) {
        return create(raster, 0, roi, DEFAULT_BIN_COUNT, pm);
    }

    public static Stx create(RasterDataNode raster, ROI roi, int binCount, ProgressMonitor pm) {
        return create(raster, 0, roi, binCount, pm);
    }

    public static Stx create(RasterDataNode raster, ROI roi, int binCount, double min, double max, ProgressMonitor pm) {
        return create(raster, 0, roi, binCount, min, max, pm);
    }

    public Stx(double min, double max, boolean intType, int[] sampleFrequencies, int resolutionLevel) {
        this(min, max, createHistogram(min, max + (intType ? 1.0 : 0.0), sampleFrequencies), resolutionLevel);
    }

    private Stx(double min, double max, Histogram histogram, int resolutionLevel) {
        this.min = min;
        this.max = max;
        this.sampleCount = computeSum(histogram.getBins(0));
        this.histogram = histogram;
        this.resolutionLevel = resolutionLevel;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return histogram.getMean()[0];
    }

    public double getStandardDeviation() {
        return histogram.getStandardDeviation()[0];
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

    private static Stx create(RasterDataNode raster, int level, ROI roi, int binCount, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing statistics", 2);
            final ExtremaOp extremaOp = new ExtremaOp();
            accumulate(raster, level, roi, extremaOp, SubProgressMonitor.create(pm, 1));

            double min = extremaOp.lowValue;
            double max = extremaOp.highValue;
            
            if (min == Double.MAX_VALUE && max == -Double.MAX_VALUE) {
                final Histogram histogram = createHistogram(1, 0, 1);
                histogram.getBins(0)[0] = 0;
                return new Stx(0.0, 1.0, histogram, level);
            }

            double off = getHighValueOffset(raster);

            final HistogramOp histogramOp = new HistogramOp(binCount, min, max + off);
            accumulate(raster, level, roi, histogramOp, SubProgressMonitor.create(pm, 1));

            // Create JAI histo, but use our "BEAM" bins
            final Histogram histogram = createHistogram(binCount, min, max + off);
            System.arraycopy(histogramOp.bins, 0, histogram.getBins(0), 0, binCount);

            return new Stx(min, max, histogram, level);
        } finally {
            pm.done();
        }
    }

    private static Stx create(RasterDataNode raster, int level, ROI roi, int binCount, double min, double max, ProgressMonitor pm) {
        double off = getHighValueOffset(raster);

        final HistogramOp histogramOp = new HistogramOp(binCount, min, max + off);
        accumulate(raster, level, roi, histogramOp, pm);

        // Create JAI histo, but use our "BEAM" bins
        final Histogram histogram = createHistogram(binCount, min, max + off);
        System.arraycopy(histogramOp.bins, 0, histogram.getBins(0), 0, binCount);

        return new Stx(min, max, histogram, level);
    }

    private static double getHighValueOffset(RasterDataNode raster) {
        return ProductData.isIntType(raster.getDataType()) ? 1.0 : 0.0;
    }

    private static Histogram createHistogram(int binCount, double min, double max) {
        return min < max ? new Histogram(binCount, min, max, 1) : new Histogram(binCount, min, min + 1e-10, 1);
    }

    private static void accumulate(RasterDataNode raster,
                                   int level,
                                   ROI roi,
                                   Op op,
                                   ProgressMonitor pm) {

        Assert.notNull(raster, "raster");
        Assert.argument(level >= 0, "level");
        Assert.argument(roi == null || level == 0, "level");
        Assert.notNull(pm, "pm");

        final RenderedImage dataImage = ImageManager.getInstance().getSourceImage(raster, level);
        final SampleModel dataSampleModel = dataImage.getSampleModel();
        if (dataSampleModel.getNumBands() != 1) {
            throw new IllegalStateException("dataSampleModel.numBands != 1");
        }
        final PixelAccessor dataAccessor = new PixelAccessor(dataSampleModel, null);

        final RenderedImage maskImage = ImageManager.getInstance().getValidMaskImage(raster, level);
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
            // todo - assert dataImage x0,y0,w,h properties equal those of maskImage (nf)
        } else {
            maskAccessor = null;
        }


        final int numXTiles = dataImage.getNumXTiles();
        final int numYTiles = dataImage.getNumYTiles();

        final int tileX1 = dataImage.getTileGridXOffset();
        final int tileY1 = dataImage.getTileGridYOffset();
        final int tileX2 = tileX1 + numXTiles - 1;
        final int tileY2 = tileY1 + numYTiles - 1;

        // todo - assert dataImage tile properties equal those of maskImage (nf)


        try {
            pm.beginTask("Computing " + op.getName(), numXTiles * numYTiles);
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                    if (pm.isCanceled()) {
                        throw new CancellationException("Process terminated by user."); /*I18N*/ 
                    }
                    final Raster dataTile = dataImage.getTile(tileX, tileY);
                    final Raster maskTile = maskImage != null ? maskImage.getTile(tileX, tileY) : null;
                    final Rectangle r = new Rectangle(dataImage.getMinX(), dataImage.getMinY(), dataImage.getWidth(), dataImage.getHeight()).intersection(dataTile.getBounds());
                    switch (dataAccessor.sampleType) {
                        case PixelAccessor.TYPE_BIT:
                        case DataBuffer.TYPE_BYTE:
                            op.accumulateDataUByte(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            op.accumulateDataUShort(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                        case DataBuffer.TYPE_SHORT:
                            op.accumulateDataShort(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                        case DataBuffer.TYPE_INT:
                            op.accumulateDataInt(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            op.accumulateDataFloat(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            op.accumulateDataDouble(dataAccessor, dataTile, maskAccessor, maskTile, r, roi);
                            break;
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private interface Op {
        String getName();

        void accumulateDataUByte(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

        void accumulateDataShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

        void accumulateDataUShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

        void accumulateDataInt(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

        void accumulateDataFloat(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

        void accumulateDataDouble(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi);

    }

    private static class ExtremaOp implements Op {
        private double lowValue;
        private double highValue;

        private ExtremaOp() {
            this.lowValue = Double.MAX_VALUE;
            this.highValue = -Double.MAX_VALUE;
        }

        public String getName() {
            return "Extrema";
        }

        public void accumulateDataUByte(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_BYTE, false);
            final byte[] data = duid.getByteData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset] & 0xff;
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }
            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public void accumulateDataUShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_USHORT, false);
            final short[] data = duid.getShortData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset] & 0xffff;
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }

            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public void accumulateDataShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_SHORT, false);
            final short[] data = duid.getShortData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }

            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public void accumulateDataInt(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_INT, false);
            final int[] data = duid.getIntData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }

            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public void accumulateDataFloat(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_FLOAT, false);
            final float[] data = duid.getFloatData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }

            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public void accumulateDataDouble(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            double lowValue = this.lowValue;
            double highValue = this.highValue;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_DOUBLE, false);
            final double[] data = duid.getDoubleData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d < lowValue) {
                            lowValue = d;
                        } else if (d > highValue) {
                            highValue = d;
                        }
                    }
                    dataPixelOffset += dataPixelStride;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset += dataLineStride;
                maskLineOffset += maskLineStride;
            }

            this.lowValue = lowValue;
            this.highValue = highValue;
        }

    }

    private static class HistogramOp implements Op {
        private final double lowValue;
        private final double highValue;
        private final double binWidth;
        private final int[] bins;

        private HistogramOp(int numBins, double lowValue, double highValue) {
            this.lowValue = lowValue;
            this.highValue = highValue;
            this.binWidth = (highValue - lowValue) / numBins;
            this.bins = new int[numBins];
        }

        public String getName() {
            return "Histogram";
        }

        public void accumulateDataUByte(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_BYTE, false);
            final byte[] data = duid.getByteData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset] & 0xff;
                        if (d >= lowValue && d < highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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

        public void accumulateDataUShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_USHORT, false);
            final short[] data = duid.getShortData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset] & 0xffff;
                        if (d >= lowValue && d < highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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

        public void accumulateDataShort(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_SHORT, false);
            final short[] data = duid.getShortData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d >= lowValue && d < highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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

        public void accumulateDataInt(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_INT, false);
            final int[] data = duid.getIntData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d >= lowValue && d < highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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

        public void accumulateDataFloat(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_FLOAT, false);
            final float[] data = duid.getFloatData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d >= lowValue && d <= highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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

        public void accumulateDataDouble(PixelAccessor dataAccessor, Raster dataTile, PixelAccessor maskAccessor, Raster maskTile, Rectangle r, ROI roi) {
            final int[] bins = this.bins;
            final double lowValue = this.lowValue;
            final double highValue = this.highValue;
            final double binWidth = this.binWidth;

            final UnpackedImageData duid = dataAccessor.getPixels(dataTile, r, DataBuffer.TYPE_DOUBLE, false);
            final double[] data = duid.getDoubleData(0);
            final int dataPixelStride = duid.pixelStride;
            final int dataLineStride = duid.lineStride;
            final int dataBandOffset = duid.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskAccessor != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            final int width = r.width;
            final int height = r.height;

            int dataLineOffset = dataBandOffset;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < height; y++) {
                int dataPixelOffset = dataLineOffset;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0) && (roi == null || roi.contains(r.x + x, r.y + y))) {
                        double d = data[dataPixelOffset];
                        if (d >= lowValue && d <= highValue) {
                            int i = (int) ((d - lowValue) / binWidth);
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
    }

}
