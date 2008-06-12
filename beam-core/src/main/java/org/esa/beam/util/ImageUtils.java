/*
 * $Id: ImageUtils.java,v 1.2 2006/12/08 13:48:37 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

// Important: make sure that we get no dependencies to
// other org.esa.beam packages here above org.esa.beam.util

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.jai.SingleBandedSampleModel;
import org.esa.beam.util.math.Quantizer;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * A utility class providing a set of static functions frequently used when working with images.
 * <p/>
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ImageUtils {

    /**
     * Converts the given rendered image into an image of the given {#link java.awt.image.BufferedImage} type.
     *
     * @param image     the source image
     * @param imageType the  {#link java.awt.image.BufferedImage} type
     * @return the buffered image of the given type
     */
    public static BufferedImage convertImage(RenderedImage image, int imageType) {
        final BufferedImage newImage;
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (imageType != BufferedImage.TYPE_CUSTOM) {
            newImage = new BufferedImage(width, height, imageType);
        } else {
            // create custom image
            final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            final ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            final WritableRaster wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3 * width, 3,
                                                                     new int[]{2, 1, 0}, null);
            newImage = new BufferedImage(cm, wr, false, null);
        }
        final Graphics2D graphics = newImage.createGraphics();
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        return newImage;
    }

    /**
     * Returns an array containing the minimum and maximum value of the native data type used to store pixel values in
     * the given image.
     *
     * @param dataType a data type as defined in <code>DataBuffer</code>
     * @see java.awt.image.DataBuffer
     */
    public static double[] getDataTypeMinMax(int dataType, double[] minmax) {
        if (minmax == null) {
            minmax = new double[2];
        }
        if (dataType == DataBuffer.TYPE_BYTE
                || dataType == DataBuffer.TYPE_INT) {
            minmax[0] = 0.0;
            minmax[1] = 255.0;
        } else if (dataType == DataBuffer.TYPE_SHORT) {
            minmax[0] = Short.MIN_VALUE;
            minmax[1] = Short.MAX_VALUE;
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            minmax[0] = 0.0;
            minmax[1] = 2.0 * Short.MAX_VALUE - 1.0;
        } else {
            minmax[0] = 0.0;
            minmax[1] = 1.0;
        }
        return minmax;
    }

    /**
     * Gets a textual representation of the supplied raster data type
     *
     * @param dataType a data type as defined in <code>DataBuffer</code>
     * @return a textual representation of the supplied raster data type
     * @see java.awt.image.DataBuffer
     */
    public static String getDataTypeName(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return UtilConstants.BUFFER_BYTE_NAME;
            case DataBuffer.TYPE_SHORT:
                return UtilConstants.BUFFER_SHORT_NAME;
            case DataBuffer.TYPE_USHORT:
                return UtilConstants.BUFFER_USHORT_NAME;
            case DataBuffer.TYPE_INT:
                return UtilConstants.BUFFER_INT_NAME;
            case DataBuffer.TYPE_FLOAT:
                return UtilConstants.BUFFER_FLOAT_NAME;
            case DataBuffer.TYPE_DOUBLE:
                return UtilConstants.BUFFER_DOUBLE_NAME;
            case DataBuffer.TYPE_UNDEFINED:
                return UtilConstants.BUFFER_UNDEFINED_NAME;
            default:
                return UtilConstants.BUFFER_UNKNOWN_NAME;
        }
    }

    /**
     * Gets a textual representation of the supplied color space type
     *
     * @param spaceType a dcolor space type as defined in <code>ColorSpace</code>
     * @return a textual representation of the color space
     * @see java.awt.color.ColorSpace
     */
    public static String getColorSpaceName(int spaceType) {
        switch (spaceType) {
            case ColorSpace.TYPE_XYZ:
                return UtilConstants.CS_TYPE_XYZ;
            case ColorSpace.TYPE_Lab:
                return UtilConstants.CS_TYPE_LAB;
            case ColorSpace.TYPE_Luv:
                return UtilConstants.CS_TYPE_LUV;
            case ColorSpace.TYPE_YCbCr:
                return UtilConstants.CS_TYPE_YCBCR;
            case ColorSpace.TYPE_Yxy:
                return UtilConstants.CS_TYPE_YXY;
            case ColorSpace.TYPE_RGB:
                return UtilConstants.CS_TYPE_RGB;
            case ColorSpace.TYPE_GRAY:
                return UtilConstants.CS_TYPE_GRAY;
            case ColorSpace.TYPE_HSV:
                return UtilConstants.CS_TYPE_HSV;
            case ColorSpace.TYPE_HLS:
                return UtilConstants.CS_TYPE_HLS;
            case ColorSpace.TYPE_CMYK:
                return UtilConstants.CS_TYPE_CMYK;
            case ColorSpace.TYPE_CMY:
                return UtilConstants.CS_TYPE_CMY;
            case ColorSpace.TYPE_2CLR:
                return UtilConstants.CS_TYPE_2CLR;
            case ColorSpace.TYPE_3CLR:
                return UtilConstants.CS_TYPE_3CLR;
            case ColorSpace.TYPE_4CLR:
                return UtilConstants.CS_TYPE_4CLR;
            case ColorSpace.TYPE_5CLR:
                return UtilConstants.CS_TYPE_5CLR;
            case ColorSpace.TYPE_6CLR:
                return UtilConstants.CS_TYPE_6CLR;
            case ColorSpace.TYPE_7CLR:
                return UtilConstants.CS_TYPE_7CLR;
            case ColorSpace.TYPE_8CLR:
                return UtilConstants.CS_TYPE_8CLR;
            case ColorSpace.TYPE_9CLR:
                return UtilConstants.CS_TYPE_9CLR;
            case ColorSpace.TYPE_ACLR:
                return UtilConstants.CS_TYPE_ACLR;
            case ColorSpace.TYPE_BCLR:
                return UtilConstants.CS_TYPE_BCLR;
            case ColorSpace.TYPE_CCLR:
                return UtilConstants.CS_TYPE_CCLR;
            case ColorSpace.TYPE_DCLR:
                return UtilConstants.CS_TYPE_DCLR;
            case ColorSpace.TYPE_ECLR:
                return UtilConstants.CS_TYPE_ECLR;
            case ColorSpace.TYPE_FCLR:
                return UtilConstants.CS_TYPE_FCLR;
            default:
                return UtilConstants.CS_TYPE_UNKNOWN;
        }
    }


    public static BufferedImage createGreyscaleColorModelImage(int width, int height, byte[] data) {
        ColorModel cm = create8BitGreyscaleColorModel();
        DataBufferByte db = new DataBufferByte(data, data.length);
        WritableRaster wr = WritableRaster.createBandedRaster(db, width, height, width, new int[]{0}, new int[]{0},
                                                              null);
        return new BufferedImage(cm, wr, false, null);
    }

    public static BufferedImage createIndexedImage(int width, int height, byte[] data, IndexColorModel cm) {
        final int numSamples = data.length;
        SampleModel sm = cm.createCompatibleSampleModel(width, height);
        DataBuffer db = new DataBufferByte(data, numSamples);
        WritableRaster wr = WritableRaster.createWritableRaster(sm, db, null);
        return new BufferedImage(cm, wr, false, null);
    }

    public static ColorModel create8BitGreyscaleColorModel() {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        return (ColorModel) new ComponentColorModel(cs, // colorSpace
                                                    new int[]{8}, // bits
                                                    false, // hasAlpha
                                                    false, // isAlphaPremultiplied
                                                    Transparency.OPAQUE, // transparency
                                                    DataBuffer.TYPE_BYTE);
    }

    /////////////////////////////////////////////////////////////////////////
    //  quantizeRasterData<Type>

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeByte} instead
     */
    public static void quantizeSamplesByte(final byte[] samples,
                                           final int min,
                                           final int max,
                                           final byte[] output,
                                           final int offset,
                                           final int stride) {
        Quantizer.quantizeByte(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeUByte} instead
     */
    public static void quantizeSamplesUByte(final byte[] samples,
                                            final int min,
                                            final int max,
                                            final byte[] output,
                                            final int offset,
                                            final int stride) {
        Quantizer.quantizeUByte(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeShort} instead
     */
    public static void quantizeSamplesShort(final short[] samples,
                                            final int min,
                                            final int max,
                                            final byte[] output,
                                            final int offset,
                                            final int stride) {
        Quantizer.quantizeShort(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeUShort} instead
     */
    public static void quantizeSamplesUShort(final short[] samples,
                                             final int min,
                                             final int max,
                                             final byte[] output,
                                             final int offset,
                                             final int stride) {
        Quantizer.quantizeUShort(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeInt} instead
     */
    public static void quantizeSamplesInt(final int[] samples,
                                          final int min,
                                          final int max,
                                          final byte[] output,
                                          final int offset,
                                          final int stride) {
        Quantizer.quantizeInt(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeUInt} instead
     */
    public static void quantizeSamplesUInt(final int[] samples,
                                           final long min,
                                           final long max,
                                           final byte[] output,
                                           final int offset,
                                           final int stride) {
        Quantizer.quantizeUInt(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeFloat} instead
     */
    public static void quantizeSamplesFloat(final float[] samples,
                                            final float min,
                                            final float max,
                                            final byte[] output,
                                            final int offset,
                                            final int stride) {
        Quantizer.quantizeFloat(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    /**
     * @deprecated in 4.0, use {@link org.esa.beam.util.math.Quantizer#quantizeDouble} instead
     */
    public static void quantizeSamplesDouble(final double[] samples,
                                             final double min,
                                             final double max,
                                             final byte[] output,
                                             final int offset,
                                             final int stride) {
        Quantizer.quantizeDouble(samples, min, max, output, offset, stride, ProgressMonitor.NULL);
    }

    public static Object getPrimitiveArray(DataBuffer dataBuffer) {
        switch (dataBuffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return ((DataBufferByte) dataBuffer).getData();
            case DataBuffer.TYPE_SHORT:
                return ((DataBufferShort) dataBuffer).getData();
            case DataBuffer.TYPE_USHORT:
                return ((DataBufferUShort) dataBuffer).getData();
            case DataBuffer.TYPE_INT:
                return ((DataBufferInt) dataBuffer).getData();
            case DataBuffer.TYPE_FLOAT:
                return ((DataBufferFloat) dataBuffer).getData();
            case DataBuffer.TYPE_DOUBLE:
                return ((DataBufferDouble) dataBuffer).getData();
            default:
                throw new IllegalArgumentException("dataBuffer");
        }
    }

    public static Object createDataBufferArray(int dataBufferType, int size) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return new byte[size];
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
                return new short[size];
            case DataBuffer.TYPE_INT:
                return new int[size];
            case DataBuffer.TYPE_FLOAT:
                return new float[size];
            case DataBuffer.TYPE_DOUBLE:
                return new double[size];
            default:
                throw new IllegalArgumentException("dataBuffer");
        }
    }

    public static SampleModel createSingleBandedSampleModel(int dataBufferType, int width, int height) {
        // Note: The SingleBandSampleModel has shown to be about 2 times faster!
        //        return RasterFactory.createPixelInterleavedSampleModel(dataBufferType,
        //                                                               width,
        //                                                               height,
        //                                                               1);
        return new SingleBandedSampleModel(dataBufferType, width, height);
    }
}
