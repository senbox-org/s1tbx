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

package com.bc.ceres.jai.opimage;

import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.operator.ScalingType;
import org.apache.commons.math3.util.FastMath;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PointOpImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.EXPONENTIAL;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LOGARITHMIC;


public final class ReinterpretOpImage extends PointOpImage {

    private static final double LOG10 = Math.log(10);

    private final double factor;
    private final double offset;
    private final ScalingType scalingType;
    private final InterpretationType interpretationType;
    private final ScalingTransform scalingTransform;

    static RenderedImage create(RenderedImage source, double factor, double offset, ScalingType scalingType,
                                InterpretationType interpretationType, Map<Object, Object> config) {
        final ImageLayout imageLayout;
        if (config != null && config.get(JAI.KEY_IMAGE_LAYOUT) instanceof ImageLayout) {
            imageLayout = (ImageLayout) config.get(JAI.KEY_IMAGE_LAYOUT);
        } else {
            final int targetDataType = ReinterpretDescriptor.getTargetDataType(source.getSampleModel().getDataType(),
                                                                               factor,
                                                                               offset,
                                                                               scalingType,
                                                                               interpretationType);
            final PixelInterleavedSampleModel sampleModel = new PixelInterleavedSampleModel(targetDataType,
                                                                                            source.getTileWidth(),
                                                                                            source.getTileHeight(),
                                                                                            1,
                                                                                            source.getTileWidth(),
                                                                                            new int[]{0});
            imageLayout = ReinterpretDescriptor.createTargetImageLayout(source, sampleModel);
        }

        return new ReinterpretOpImage(source, imageLayout, config, factor, offset, scalingType, interpretationType);
    }

    private ReinterpretOpImage(RenderedImage source, ImageLayout imageLayout, Map<Object, Object> config,
                               double factor, double offset, ScalingType scalingType,
                               InterpretationType interpretationType) {
        super(source, imageLayout, config, true);
        this.factor = factor;
        this.offset = offset;
        this.scalingType = scalingType;
        this.interpretationType = interpretationType;
        this.scalingTransform = scalingType == EXPONENTIAL ? new Pow10() : scalingType == LOGARITHMIC ? new Log10() : null;
        // set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    @Override
    protected void computeRect(Raster[] sourceRasters, WritableRaster targetRaster, Rectangle targetRectangle) {
        if (scalingType == LINEAR && factor == 1.0 && offset == 0.0) {
            reformat(sourceRasters[0], targetRaster, targetRectangle);
        } else {
            rescale(sourceRasters[0], targetRaster, targetRectangle);
        }
    }

    private void rescale(Raster sourceRaster, WritableRaster targetRaster, Rectangle targetRectangle) {
        final int sourceDataType = sourceRaster.getSampleModel().getDataType();
        final int targetDataType = targetRaster.getSampleModel().getDataType();
        final PixelAccessor sourceAcc = new PixelAccessor(getSourceImage(0));
        final PixelAccessor targetAcc = new PixelAccessor(this);
        final UnpackedImageData sourcePixels;
        final UnpackedImageData targetPixels;

        sourcePixels = sourceAcc.getPixels(sourceRaster, targetRectangle, sourceDataType, false);
        targetPixels = targetAcc.getPixels(targetRaster, targetRectangle, targetDataType, true);

        switch (sourceDataType) {
            case DataBuffer.TYPE_BYTE:
                if (interpretationType == ReinterpretDescriptor.INTERPRET_BYTE_SIGNED) {
                    rescaleSByte(sourcePixels, targetPixels, targetRectangle);
                } else {
                    rescaleByte(sourcePixels, targetPixels, targetRectangle);
                }
                break;
            case DataBuffer.TYPE_USHORT:
                rescaleUShort(sourcePixels, targetPixels, targetRectangle);
                break;
            case DataBuffer.TYPE_SHORT:
                rescaleShort(sourcePixels, targetPixels, targetRectangle);
                break;
            case DataBuffer.TYPE_INT:
                if (interpretationType == ReinterpretDescriptor.INTERPRET_INT_UNSIGNED) {
                    rescaleUInt(sourcePixels, targetPixels, targetRectangle);
                } else {
                    rescaleInt(sourcePixels, targetPixels, targetRectangle);
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                rescaleFloat(sourcePixels, targetPixels, targetRectangle);
                break;
            case DataBuffer.TYPE_DOUBLE:
                rescaleDouble(sourcePixels, targetPixels, targetRectangle);
                break;
        }

        targetAcc.setPixels(targetPixels);
    }

    private void reformat(Raster sourceRaster, WritableRaster targetRaster, Rectangle targetRectangle) {
        final int sourceDataType = sourceRaster.getSampleModel().getDataType();
        final int targetDataType = targetRaster.getSampleModel().getDataType();
        final PixelAccessor sourceAcc = new PixelAccessor(getSourceImage(0));
        final PixelAccessor targetAcc = new PixelAccessor(this);
        final UnpackedImageData sourcePixels;
        final UnpackedImageData targetPixels;

        sourcePixels = sourceAcc.getPixels(sourceRaster, targetRectangle, sourceDataType, false);

        switch (sourceDataType) {
            case DataBuffer.TYPE_BYTE:
                if (interpretationType == ReinterpretDescriptor.INTERPRET_BYTE_SIGNED) {
                    targetPixels = targetAcc.getPixels(targetRaster, targetRectangle, targetDataType, true);
                    reformatSByte(sourcePixels, targetPixels, targetRectangle);
                    break;
                }
            case DataBuffer.TYPE_INT:
                if (interpretationType == ReinterpretDescriptor.INTERPRET_INT_UNSIGNED) {
                    targetPixels = targetAcc.getPixels(targetRaster, targetRectangle, targetDataType, true);
                    reformatUInt(sourcePixels, targetPixels, targetRectangle);
                    break;
                }
            default:
                targetPixels = sourcePixels;
        }

        targetAcc.setPixels(targetPixels);
    }

    private void reformatSByte(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                               Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final byte[] sourceData = sourcePixels.getByteData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final short[] targetData = targetPixels.getShortData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int sourcePixelOffset = sourceLineOffset;
            sourceLineOffset += sourceLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final short v = sourceData[sourcePixelOffset];
                targetData[targetPixelOffset] = v;

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            } // next x
        } // next y
    }

    private void reformatUInt(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                              Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final int[] sourceData = sourcePixels.getIntData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        for (int y = 0; y < h; y++) {
            int sourcePixelOffset = sourceLineOffset;
            sourceLineOffset += sourceLineStride;

            int targetPixelOffset = targetLineOffset;
            targetLineOffset += targetLineStride;

            for (int x = 0; x < w; x++) {
                final double v = sourceData[sourcePixelOffset] & 0xFFFFFFFFL;
                targetData[targetPixelOffset] = v;

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            } // next x
        } // next y
    }

    private void rescaleByte(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                             Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final byte[] sourceData = sourcePixels.getByteData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFF;
                    targetData[targetPixelOffset] = (float) st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFF;
                    targetData[targetPixelOffset] = (float) (factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleSByte(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                              Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final byte[] sourceData = sourcePixels.getByteData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) (factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleUShort(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                               Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final short[] sourceData = sourcePixels.getShortData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFF;
                    targetData[targetPixelOffset] = (float) st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFF;
                    targetData[targetPixelOffset] = (float) (factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleShort(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                              Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final short[] sourceData = sourcePixels.getShortData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) (factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleInt(UnpackedImageData sourcePixels, UnpackedImageData targetPixels, Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final int[] sourceData = sourcePixels.getIntData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = factor * v + offset;

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleUInt(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                             Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final int[] sourceData = sourcePixels.getIntData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFFFFFFL;
                    targetData[targetPixelOffset] = st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFFFFFFL;
                    targetData[targetPixelOffset] = factor * v + offset;

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleFloat(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                              Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final float[] sourceData = sourcePixels.getFloatData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final float[] targetData = targetPixels.getFloatData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final float v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final float v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) (factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private void rescaleDouble(UnpackedImageData sourcePixels, UnpackedImageData targetPixels,
                               Rectangle targetRectangle) {
        final int sourceLineStride = sourcePixels.lineStride;
        final int sourcePixelStride = sourcePixels.pixelStride;
        final double[] sourceData = sourcePixels.getDoubleData(0);

        final int targetLineStride = targetPixels.lineStride;
        final int targetPixelStride = targetPixels.pixelStride;
        final double[] targetData = targetPixels.getDoubleData(0);

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        int sourceLineOffset = sourcePixels.bandOffsets[0];
        int targetLineOffset = targetPixels.bandOffsets[0];

        if (scalingTransform != null) {
            ScalingTransform st = this.scalingTransform;
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = st.transform(factor * v + offset);

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        } else {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = factor * v + offset;

                    sourcePixelOffset += sourcePixelStride;
                    targetPixelOffset += targetPixelStride;
                } // next x
            } // next y
        }
    }

    private static ImageLayout createTargetImageLayout(RenderedImage source, SampleModel sampleModel) {
        final int w = source.getWidth();
        final int h = source.getHeight();

        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setWidth(w);
        imageLayout.setHeight(h);
        imageLayout.setSampleModel(sampleModel);

        return imageLayout;
    }

    /**
     * Implementation code of this interface shall be easily in-lined by the compiler.
     */
    private interface ScalingTransform {
        double transform(double x);
    }

    private final class Pow10 implements ScalingTransform {
        public double transform(double x) {
            // This is ~500 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            //return Math.exp(LOG10 * x);
            // This is ~700 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            //return Math.pow(10.0, x);
            // This is ~300 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            return FastMath.exp(LOG10 * x);
        }
    }

    private final class Log10 implements ScalingTransform {
        public double transform(double x) {
            // This is slightly below 300 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            return Math.log10(x);
            // This is slightly above 300 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            //return Math.log(x) / LOG10;
            // This is ~900 ms per 4 mega-pixels on my Intel i7 2.8 GHz CPU
            //return FastMath.log10(x);
        }
    }
}
