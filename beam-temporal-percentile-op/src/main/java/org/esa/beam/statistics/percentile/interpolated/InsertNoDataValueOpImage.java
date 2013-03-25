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
package org.esa.beam.statistics.percentile.interpolated;

import org.esa.beam.framework.gpf.internal.OperatorContext;

import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

final class InsertNoDataValueOpImage extends PointOpImage {
    private final double noDataValue;
    private final RasterFormatTag maskRasterFormatTag;

    InsertNoDataValueOpImage(RenderedImage sourceImage, RenderedImage maskImage, double noDataValue) {
        super(sourceImage, maskImage, null, null, true);
        this.noDataValue = noDataValue;
        int compatibleTagId = RasterAccessor.findCompatibleTag(null, maskImage.getSampleModel());
        maskRasterFormatTag = new RasterFormatTag(maskImage.getSampleModel(), compatibleTagId);
        OperatorContext.setTileCache(this);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor m = new RasterAccessor(sources[1], destRect, maskRasterFormatTag, getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[2], getColorModel());
        switch (d.getDataType()) {
            case 0: // '\0'
                computeRectByte(s, m, d, (byte) noDataValue);
                break;

            case 1: // '\001'
            case 2: // '\002'
                computeRectShort(s, m, d, (short) noDataValue);
                break;

            case 3: // '\003'
                computeRectInt(s, m, d, (int) noDataValue);
                break;
            case 4: // '\004'
                computeRectFloat(s, m, d, (float) noDataValue);
                break;

            case 5: // '\005'
                computeRectDouble(s, m, d, noDataValue);
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
