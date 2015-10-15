package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.gpf.OperatorException;

import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;

class MeanOpImage extends PointOpImage {

    public MeanOpImage(Vector<RenderedImage> sources) {
        super(sources, null, null, true);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor[] sourceRasterAccessors = new RasterAccessor[sources.length];
        for (int i = 0; i < sources.length; i++) {
            sourceRasterAccessors[i] = new RasterAccessor(sources[i], destRect, formatTags[i], getSourceImage(i).getColorModel());
        }
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[sources.length], getColorModel());
        switch (d.getDataType()) {
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(sourceRasterAccessors, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(sourceRasterAccessors, d);
            break;
        default:
            throw new OperatorException("Unable to compute raster for non floating number data type");
        }
        d.copyDataToRaster();
    }

    private void computeRectFloat(RasterAccessor[] sourceRasterAccessors, RasterAccessor dst) {

        final RasterAccessor src = sourceRasterAccessors[0];
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] srcData = new float[sourceRasterAccessors.length][];
        for (int i = 0; i < sourceRasterAccessors.length; i++) {
            RasterAccessor sourceRasterAccessor = sourceRasterAccessors[i];
            float[][] sData = sourceRasterAccessor.getFloatDataArrays();
            srcData[i] = sData[0];
        }

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();


        float[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                float sum = Float.NaN;
                int count = 0;
                for (float[] s : srcData) {
                    final float currentValue = s[sPixelOffset];
                    if (!Float.isNaN(currentValue)) {
                        if (count == 0) {
                            sum = currentValue;
                        } else {
                            sum += currentValue;
                        }
                        count++;
                    }
                }
                if (count == 0) {
                    d[dPixelOffset] = Float.NaN;
                } else {
                    d[dPixelOffset] = sum / count;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor[] sourceRasterAccessors, RasterAccessor dst) {
        final RasterAccessor src = sourceRasterAccessors[0];
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        double [][] srcData = new double[sourceRasterAccessors.length][];
        for (int i = 0; i < sourceRasterAccessors.length; i++) {
            RasterAccessor sourceRasterAccessor = sourceRasterAccessors[i];
            double[][] sData = sourceRasterAccessor.getDoubleDataArrays();
            srcData[i] = sData[0];
        }

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        double [] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                double sum = Double.NaN;
                int count = 0;
                for (double[] s : srcData) {
                    final double currentValue = s[sPixelOffset];
                    if (!Double.isNaN(currentValue)) {
                        if (count == 0) {
                            sum = currentValue;
                        } else {
                            sum += currentValue;
                        }
                        count++;
                    }
                }
                if (count == 0) {
                    d[dPixelOffset] = Double.NaN;
                } else {
                    d[dPixelOffset] = sum / count;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }
}
