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

import com.bc.ceres.jai.GeneralFilterFunction;

import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;


/**
 * An OpImage class to perform convolution on a source image.
 * <p> This class implements a convolution operation. Convolution is a
 * spatial operation that computes each output sample by multiplying
 * elements of a kernel with the samples surrounding a particular
 * source sample.
 * <p> For each destination sample, the kernel is rotated 180 degrees
 * and its "key element" is placed over the source pixel corresponding
 * with the destination pixel.  The kernel elements are multiplied
 * with the source pixels under them, and the resulting products are
 * summed together to produce the destination sample value.
 * <p> Example code for the convolution operation on a single sample
 * dst[x][y] is as follows. First your original kernel is rotated
 * by 180 degrees, then the following -
 * assuming the kernel is of size M rows x N columns
 * and the rotated kernel's key element is at position (xKey, yKey):
 * <pre>
 * dst[x][y] = 0;
 * for (int i = -xKey; i &lt; M - xKey; i++) {
 *     for (int j = -yKey; j &lt; N - yKey; j++) {
 *         dst[x][y] += src[x + i][y + j] * kernel[xKey + i][yKey + j];
 *     }
 * }
 * </pre>
 * <p> Convolution, or any neighborhood operation, leaves a band of
 * pixels around the edges undefined, i.e., for a 3x3 kernel, only
 * four kernel elements and four source pixels contribute to the
 * destination pixel located at (0,0).  Such pixels are not includined
 * in the destination image.  A BorderOpImage may be used to add an
 * appropriate border to the source image in order to avoid shrinkage
 * of the image boundaries.
 * <p> The Kernel cannot be bigger in any dimension than the image data.
 *
 * @see KernelJAI
 */
public final class GeneralFilterOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the convolve operation.
     */
    private final GeneralFilterFunction filterFunction;

    /**
     * Creates a ConvolveOpImage given a ParameterBlock containing the image
     * source and pre-rotated convolution kernel.  The image dimensions are
     * derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source         a RenderedImage.
     * @param extender       a BorderExtender, or null.
     * @param config         the image configuration.
     * @param layout         an ImageLayout optionally containing the tile grid layout,
     *                       SampleModel, and ColorModel, or null.
     * @param filterFunction the pre-rotated convolution KernelJAI.
     */
    public GeneralFilterOpImage(RenderedImage source,
                                BorderExtender extender,
                                Map config,
                                ImageLayout layout,
                                GeneralFilterFunction filterFunction) {
        super(source,
              layout,
              config,
              true,
              extender,
              filterFunction.getLeftPadding(),
              filterFunction.getRightPadding(),
              filterFunction.getTopPadding(),
              filterFunction.getBottomPadding());

        this.filterFunction = filterFunction;
    }

    /**
     * Performs convolution on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);


        RasterAccessor srcAccessor =
                new RasterAccessor(source, srcRect,
                                   formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect,
                                   formatTags[1], getColorModel());

        switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(srcAccessor, dstAccessor);
                break;

            default:
        }

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = ((int) srcData[imageOffset] & 0xff);
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    int val = (int) filterFunction.filter(fdata);
                    if (val < 0) {
                        val = 0;
                    } else if (val > 255) {
                        val = 255;
                    }
                    dstData[dstPixelOffset] = (byte) val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    int val = (int) filterFunction.filter(fdata);
                    if (val < Short.MIN_VALUE) {
                        val = Short.MIN_VALUE;
                    } else if (val > Short.MAX_VALUE) {
                        val = Short.MAX_VALUE;
                    }

                    dstData[dstPixelOffset] = (short) val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = (srcData[imageOffset] & 0xffff);
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }
                    int val = (int) filterFunction.filter(fdata);
                    if (val < 0) {
                        val = 0;
                    } else if (val > 0xffff) {
                        val = 0xffff;
                    }

                    dstData[dstPixelOffset] = (short) val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void intLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    dstData[dstPixelOffset] = (int) filterFunction.filter(fdata);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    dstData[dstPixelOffset] = filterFunction.filter(fdata);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int fw = filterFunction.getWidth();
        int fh = filterFunction.getHeight();
        float[] fdata = new float[fw * fh];

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++) {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int filterVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < fh; u++) {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < fw; v++) {
                            fdata[filterVerticalOffset + v] = (float) srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        filterVerticalOffset += fw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    dstData[dstPixelOffset] = filterFunction.filter(fdata);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }
}
