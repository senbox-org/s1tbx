package com.bc.ceres.jai.opimage;

import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.operator.ScalingType;

import javax.media.jai.ImageLayout;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PointOpImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;


public final class ReinterpretOpImage extends PointOpImage {

    private static final double LOG10 = Math.log(10);

    private final double factor;
    private final double offset;
    private final ScalingType scalingType;
    private final InterpretationType interpretationType;

    static RenderedImage create(RenderedImage source, double factor, double offset, ScalingType scalingType,
                                InterpretationType interpretation, Map config) {
        final boolean rescale = scalingType == ReinterpretDescriptor.EXPONENTIAL || factor != 1.0 || offset != 0.0;
        final ImageLayout imageLayout = createTargetImageLayout(source, rescale, interpretation);

        return new ReinterpretOpImage(source, imageLayout, config, factor, offset, scalingType, interpretation);
    }

    private ReinterpretOpImage(RenderedImage source, ImageLayout imageLayout, Map config,
                               double factor, double offset, ScalingType scalingType,
                               InterpretationType interpretationType) {
        super(source, imageLayout, config, true);
        this.factor = scalingType == ReinterpretDescriptor.EXPONENTIAL ? LOG10 * factor : factor;
        this.offset = scalingType == ReinterpretDescriptor.EXPONENTIAL ? LOG10 * offset : offset;
        this.scalingType = scalingType;
        this.interpretationType = interpretationType;
        // set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    @Override
    protected void computeRect(Raster[] sourceRasters, WritableRaster targetRaster, Rectangle targetRectangle) {
        if (scalingType == ReinterpretDescriptor.EXPONENTIAL || factor != 1.0 || offset != 0.0) {
            rescale(sourceRasters[0], targetRaster, targetRectangle);
        } else {
            reformat(sourceRasters[0], targetRaster, targetRectangle);
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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFF;
                    targetData[targetPixelOffset] = (float) Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFF;
                    targetData[targetPixelOffset] = (float) Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset] & 0xFFFFFFFFL;
                    targetData[targetPixelOffset] = Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final float v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = (float) Math.exp(factor * v + offset);

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

        if (scalingType == ReinterpretDescriptor.EXPONENTIAL) {
            for (int y = 0; y < h; y++) {
                int sourcePixelOffset = sourceLineOffset;
                sourceLineOffset += sourceLineStride;

                int targetPixelOffset = targetLineOffset;
                targetLineOffset += targetLineStride;

                for (int x = 0; x < w; x++) {
                    final double v = sourceData[sourcePixelOffset];
                    targetData[targetPixelOffset] = Math.exp(factor * v + offset);

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

    private static ImageLayout createTargetImageLayout(RenderedImage source,
                                                       boolean rescale,
                                                       InterpretationType interpretationType) {
        final int sourceDataType = source.getSampleModel().getDataType();
        final int w = source.getWidth();
        final int h = source.getHeight();

        final int targetDataType = getTargetDataType(sourceDataType, rescale, interpretationType);
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setWidth(w);
        imageLayout.setHeight(h);
        imageLayout.setSampleModel(new PixelInterleavedSampleModel(targetDataType, w, h, 1, w, new int[]{0}));

        return imageLayout;
    }

    private static int getTargetDataType(int sourceDataType, boolean rescale, InterpretationType interpretationType) {
        if (rescale) {
            switch (sourceDataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_FLOAT:
                    return DataBuffer.TYPE_FLOAT;
                case DataBuffer.TYPE_INT:
                case DataBuffer.TYPE_DOUBLE:
                    return DataBuffer.TYPE_DOUBLE;
                default:
                    return DataBuffer.TYPE_UNDEFINED;
            }
        } else {
            switch (sourceDataType) {
                case DataBuffer.TYPE_BYTE:
                    if (interpretationType == ReinterpretDescriptor.INTERPRET_BYTE_SIGNED) {
                        return DataBuffer.TYPE_SHORT;
                    }
                case DataBuffer.TYPE_INT:
                    if (interpretationType == ReinterpretDescriptor.INTERPRET_INT_UNSIGNED) {
                        return DataBuffer.TYPE_DOUBLE;
                    }
                default:
                    return sourceDataType;
            }
        }
    }
}
