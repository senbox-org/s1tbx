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

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
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
 * @see javax.media.jai.KernelJAI
 */
public final class PaintOpImage extends PointOpImage {

    private final static boolean DEBUG = false;

    /**
     * The paint color.
     */
    private Color paintColor;

    /**
     * Whether alpha is the first band.
     */
    private boolean alphaIsFirst;

    // dependent helper
    private final int[] srcBands;
    // dependent helper
    private int[] paintPixel;

    /**
     * Creates a ConvolveOpImage given a ParameterBlock containing the image
     * source and pre-rotated convolution kernel.  The image dimensions are
     * derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source0      a source
     * @param source1      a alpha mask for painting
     * @param config       the image configuration.
     * @param layout       an ImageLayout optionally containing the tile grid layout,
     *                     SampleModel, and ColorModel, or null.
     * @param paintColor   The paint color.
     * @param alphaIsFirst Whether alpha is the first band
     */
    public PaintOpImage(RenderedImage source0,
                        RenderedImage source1,
                        Map config,
                        ImageLayout layout,
                        Color paintColor,
                        boolean alphaIsFirst) {
        super(source0,
              source1,
              layout,
              config,
              true);
        if (source0.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException("source0 must be of type BYTE");
        }
        if (source1.getSampleModel().getNumBands() > 4) {
            throw new IllegalArgumentException("source0 must have no more than 4 bands");
        }
        if (source1.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException("source1 must be of type BYTE");
        }
        if (source1.getSampleModel().getNumBands() != 1) {
            throw new IllegalArgumentException("source1 must only have one band");
        }

        SampleModel sm = source0.getSampleModel();
        int numDestBands = Math.max(sm.getNumBands(), paintColor.getAlpha() == 255 ? 3 : 4);

        if (sampleModel.getNumBands() != numDestBands) {
            sampleModel = RasterFactory.createComponentSampleModel(sampleModel,
                                                                   sm.getDataType(),
                                                                   tileWidth,
                                                                   tileHeight,
                                                                   numDestBands);
            if (colorModel != null) {
                if (!colorModel.isCompatibleSampleModel(sampleModel)) {
                    colorModel = createColorModel(sampleModel);
                }
            }
        }

        this.paintColor = paintColor;
        this.alphaIsFirst = alphaIsFirst;

        int numSourceBands = source0.getSampleModel().getNumBands();
        srcBands = new int[numDestBands];
        int iOffset;
        if (numDestBands > numSourceBands && alphaIsFirst) {
            iOffset = numDestBands - numSourceBands;
        } else {
            iOffset = 0;
        }
        Arrays.fill(srcBands, -1);
        for (int i = 0; i < numSourceBands; i++) {
            srcBands[i + iOffset] = i;
        }

        if (numDestBands == 3) {
            paintPixel = new int[]{
                    paintColor.getRed(),
                    paintColor.getGreen(),
                    paintColor.getBlue(),
            };
        } else {
            if (alphaIsFirst) {
                paintPixel = new int[]{
                        paintColor.getAlpha(),
                        paintColor.getRed(),
                        paintColor.getGreen(),
                        paintColor.getBlue(),
                };
            } else {
                paintPixel = new int[]{
                        paintColor.getRed(),
                        paintColor.getGreen(),
                        paintColor.getBlue(),
                        paintColor.getAlpha(),
                };
            }
        }

        if (DEBUG) {
            System.out.println("PaintOpImage: " + this);
            System.out.println("  colorModel = " + colorModel.getClass());
            System.out.println("  sampleModel = " + sampleModel.getClass());
            System.out.println("  paintColor = " + this.paintColor);
            System.out.println("  alphaIsFirst = " + this.alphaIsFirst);
            System.out.println("  paintPixel = " + Arrays.toString(paintPixel));
            System.out.println("  srcBands = " + Arrays.toString(srcBands));
            if (source0.getSampleModel() instanceof ComponentSampleModel) {
                ComponentSampleModel csm = (ComponentSampleModel) source0.getSampleModel();
                int[] srcBandOffsets = csm.getBandOffsets();
                System.out.println("  srcBandOffsets = " + Arrays.toString(srcBandOffsets));
            }
            if (sampleModel instanceof ComponentSampleModel) {
                ComponentSampleModel csm = (ComponentSampleModel) sampleModel;
                int[] dstBandOffsets = csm.getBandOffsets();
                System.out.println("  dstBandOffsets = " + Arrays.toString(dstBandOffsets));
            }
        }


        permitInPlaceOperation();
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

        Raster source0 = sources[0];
        Raster source1 = sources[1];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src0Accessor =
                new RasterAccessor(source0, srcRect,
                                   formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor src1Accessor =
                new RasterAccessor(source1, srcRect,
                                   formatTags[1], getSourceImage(1).getColorModel());
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect,
                                   formatTags[2], getColorModel());

        byteLoop(src0Accessor, src1Accessor, dstAccessor);

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src0, RasterAccessor src1, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
        int snumBands = src0.getNumBands();

        int paintAlpha = paintColor.getAlpha();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte src0DataArrays[][] = src0.getByteDataArrays();
        int src0BandOffsets[] = src0.getBandOffsets();
        int src0PixelStride = src0.getPixelStride();
        int src0ScanlineStride = src0.getScanlineStride();

        byte src1DataArrays[][] = src1.getByteDataArrays();
        int src1BandOffsets[] = src1.getBandOffsets();
        int src1PixelStride = src1.getPixelStride();
        int src1ScanlineStride = src1.getScanlineStride();

        for (int dstBand = 0; dstBand < dnumBands; dstBand++) {
            int srcBand = srcBands[dstBand];
            byte src0Data[] = srcBand >= 0 ? src0DataArrays[srcBand] : null;
            byte src1Data[] = src1DataArrays[0];
            byte dstData[] = dstDataArrays[dstBand];
            int src0ScanlineOffset = dstBand < snumBands ? src0BandOffsets[dstBand] : 0;
            int src1ScanlineOffset = src1BandOffsets[0];
            int dstScanlineOffset = dstBandOffsets[dstBand];
            for (int y = 0; y < dheight; y++) {
                if (src0Data != null) {
                    int src0PixelOffset = src0ScanlineOffset;
                    int src1PixelOffset = src1ScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int x = 0; x < dwidth; x++) {
                        int src0Val = (int) src0Data[src0PixelOffset] & 0xff;
                        int src1Val = (int) src1Data[src1PixelOffset] & 0xff;
                        int dstVal = (src1Val * src0Val + (255 - src1Val) * paintPixel[dstBand]) / 255;
                        dstData[dstPixelOffset] = (byte) dstVal;
                        src0PixelOffset += src0PixelStride;
                        src1PixelOffset += src1PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    src0ScanlineOffset += src0ScanlineStride;
                    src1ScanlineOffset += src1ScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                } else {
                    // Source has no alpha, compute one from paint color and mask
                    int src1PixelOffset = src1ScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        int src0Val = 255;
                        int src1Val = (int) src1Data[src1PixelOffset] & 0xff;
                        int dstVal = (src1Val * src0Val + (255 - src1Val) * paintPixel[dstBand]) / 255;
                        dstData[dstPixelOffset] = (byte) dstVal;
                        src1PixelOffset += src1PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    src1ScanlineOffset += src1ScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

}
