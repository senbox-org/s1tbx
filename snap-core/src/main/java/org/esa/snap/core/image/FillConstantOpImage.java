/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * An image to replace values in the source image controlled by a mask.
 */
public final class FillConstantOpImage extends PointOpImage {

    private final Number fillValue;
    private final RasterFormatTag maskFormatTag;
    private final RasterFormatTag[] srcDestFormatTags;

    /**
     * Where the mask image is set the original values in the source image are preserved.
     * Otherwise the values are replaced by the no-data value.
     *
     * @param sourceImage The source image.
     * @param maskImage   The mask image. This mask prevents pixel values from being overwritten by fill value (where mask != 0).
     * @param fillValue   The value to replace the original ones.
     */
    public FillConstantOpImage(RenderedImage sourceImage, RenderedImage maskImage, Number fillValue) {
        super(sourceImage, maskImage, createImageLayout(sourceImage, fillValue), null, true);
        this.fillValue = fillValue;
        int compatibleTagId = RasterAccessor.findCompatibleTag(null, maskImage.getSampleModel());
        maskFormatTag = new RasterFormatTag(maskImage.getSampleModel(), compatibleTagId);
        srcDestFormatTags = RasterAccessor.findCompatibleTags(new RenderedImage[]{sourceImage}, this);
    }

    private static ImageLayout createImageLayout(RenderedImage sourceImage, Number fillValue) {
        int targetDataType = Math.max(sourceImage.getSampleModel().getDataType(), DataBufferUtils.getDataBufferType(fillValue));
        SingleBandedSampleModel sampleModel = new SingleBandedSampleModel(targetDataType,
                                                                          sourceImage.getTileWidth(),
                                                                          sourceImage.getTileHeight());
        return new ImageLayout(sourceImage).setSampleModel(sampleModel);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterAccessor s = new RasterAccessor(sources[0], destRect, srcDestFormatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor m = new RasterAccessor(sources[1], destRect, maskFormatTag, getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, srcDestFormatTags[1], getColorModel());
        switch (d.getDataType()) {
            case 0: // '\0'
                computeRectByte(s, m, d, fillValue.byteValue());
                break;

            case 1: // '\001'
            case 2: // '\002'
                computeRectShort(s, m, d, fillValue.shortValue());
                break;

            case 3: // '\003'
                computeRectInt(s, m, d, fillValue.intValue());
                break;
            case 4: // '\004'
                computeRectFloat(s, m, d, fillValue.floatValue());
                break;

            case 5: // '\005'
                computeRectDouble(s, m, d, fillValue.doubleValue());
                break;
        }
        d.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src, RasterAccessor mask, RasterAccessor dst, byte rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        byte[][] sData = src.getByteDataArrays();

        int mLineStride = mask.getScanlineStride();
        int mPixelStride = mask.getPixelStride();
        int[] mBandOffsets = mask.getBandOffsets();
        byte[][] mData = mask.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        byte[] s = sData[0];
        byte[] m = mData[0];
        byte[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int mLineOffset = mBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int mPixelOffset = mLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            mLineOffset += mLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (m[mPixelOffset] != 0) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = rValue;
                }
                sPixelOffset += sPixelStride;
                mPixelOffset += mPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor src, RasterAccessor mask, RasterAccessor dst, short rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int mLineStride = mask.getScanlineStride();
        int mPixelStride = mask.getPixelStride();
        int[] mBandOffsets = mask.getBandOffsets();
        byte[][] mData = mask.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        short[] s = sData[0];
        byte[] m = mData[0];
        short[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int mLineOffset = mBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int mPixelOffset = mLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            mLineOffset += mLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (m[mPixelOffset] != 0) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = rValue;
                }
                sPixelOffset += sPixelStride;
                mPixelOffset += mPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor src, RasterAccessor mask, RasterAccessor dst, int rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        int[][] sData = src.getIntDataArrays();

        int mLineStride = mask.getScanlineStride();
        int mPixelStride = mask.getPixelStride();
        int[] mBandOffsets = mask.getBandOffsets();
        byte[][] mData = mask.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        int[] s = sData[0];
        byte[] m = mData[0];
        int[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int mLineOffset = mBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int mPixelOffset = mLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            mLineOffset += mLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (m[mPixelOffset] != 0) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = rValue;
                }
                sPixelOffset += sPixelStride;
                mPixelOffset += mPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor mask, RasterAccessor dst, float rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] sData = src.getFloatDataArrays();

        int mLineStride = mask.getScanlineStride();
        int mPixelStride = mask.getPixelStride();
        int[] mBandOffsets = mask.getBandOffsets();
        byte[][] mData = mask.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        float[] s = sData[0];
        byte[] m = mData[0];
        float[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int mLineOffset = mBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int mPixelOffset = mLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            mLineOffset += mLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (m[mPixelOffset] != 0) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = rValue;
                }
                sPixelOffset += sPixelStride;
                mPixelOffset += mPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor mask, RasterAccessor dst, double rValue) {
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        double[][] sData = src.getDoubleDataArrays();

        int mLineStride = mask.getScanlineStride();
        int mPixelStride = mask.getPixelStride();
        int[] mBandOffsets = mask.getBandOffsets();
        byte[][] mData = mask.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        double[] s = sData[0];
        byte[] m = mData[0];
        double[] d = dData[0];
        int sLineOffset = sBandOffsets[0];
        int mLineOffset = mBandOffsets[0];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dheight; h++) {
            int sPixelOffset = sLineOffset;
            int mPixelOffset = mLineOffset;
            int dPixelOffset = dLineOffset;
            sLineOffset += sLineStride;
            mLineOffset += mLineStride;
            dLineOffset += dLineStride;
            for (int w = 0; w < dwidth; w++) {
                if (m[mPixelOffset] != 0) {
                    d[dPixelOffset] = s[sPixelOffset];
                } else {
                    d[dPixelOffset] = rValue;
                }
                sPixelOffset += sPixelStride;
                mPixelOffset += mPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }
}
