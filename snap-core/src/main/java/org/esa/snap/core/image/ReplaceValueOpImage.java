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
package org.esa.snap.core.image;


import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

/**
 * This image replaces one value by an other value in the given source image.
 */
public final class ReplaceValueOpImage extends PointOpImage {

    private final Number oldValue;
    private final Number newValue;

    /**
     * The {@code valueToBeReplaced} are replaced by {@code replaceValue} within the {@code sourceImage}
     *
     * @param sourceImage The source image.
     * @param oldValue    The value to be replaced.
     * @param newValue    The value replacing the old value.
     * @param targetType  the data type of the resulting image
     */
    public ReplaceValueOpImage(RenderedImage sourceImage, Number oldValue, Number newValue, int targetType) {
        super(sourceImage, createImageLayout(sourceImage, targetType), null, true);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    private static ImageLayout createImageLayout(RenderedImage sourceImage, int targetDataType) {
        SingleBandedSampleModel sampleModel = new SingleBandedSampleModel(targetDataType,
                                                                          sourceImage.getTileWidth(),
                                                                          sourceImage.getTileHeight());
        return new ImageLayout(sourceImage).setSampleModel(sampleModel);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        switch (d.getDataType()) {
            case 0: // '\0'
                computeRectByte(s, d, oldValue.byteValue(), newValue.byteValue());
                break;

            case 1: // '\001'
            case 2: // '\002'
                computeRectShort(s, d, oldValue.shortValue(), newValue.shortValue());
                break;

            case 3: // '\003'
                computeRectInt(s, d, oldValue.intValue(), newValue.intValue());
                break;
            case 4: // '\004'
                computeRectFloat(s, d, oldValue.floatValue(), newValue.floatValue());
                break;

            case 5: // '\005'
                computeRectDouble(s, d, oldValue.doubleValue(), newValue.doubleValue());
                break;
        }
        d.copyDataToRaster();
    }


    private void computeRectByte(RasterAccessor src, RasterAccessor dst, byte value, byte replacement) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        byte[][] sData = src.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        byte[] s = sData[0];
        byte[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (s[sPixelOffset] != value) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = replacement;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor src, RasterAccessor dst, short value, short replacement) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        short[] s = sData[0];
        short[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (s[sPixelOffset] != value) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = replacement;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor src, RasterAccessor dst, int value, int replacement) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        int[][] sData = src.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        int[] s = sData[0];
        int[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (s[sPixelOffset] != value) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = replacement;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor dst, float value, float replacement) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] sData = src.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        float[] s = sData[0];
        float[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (!(Math.abs(s[sPixelOffset] - value) <= 1.0e-6f)) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = replacement;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor dst, double value, double replacement) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        double[][] sData = src.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        double[] s = sData[0];
        double[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (!(Math.abs(s[sPixelOffset] - value) <= 1.0e-6f)) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = replacement;
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

}
