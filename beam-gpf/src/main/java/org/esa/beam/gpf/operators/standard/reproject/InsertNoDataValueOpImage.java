/*
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.gpf.operators.standard.reproject;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.JAI;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

final class InsertNoDataValueOpImage extends PointOpImage {
    private final double noDataValue;
    private final RenderedImage maskImage;
    private final RasterFormatTag maskRasterFormatTag;
    
    InsertNoDataValueOpImage(RenderedImage sourceImage, RenderedImage maskImage, double noDataValue) {
        super(sourceImage, null, null, true);
        this.maskImage = maskImage;
        this.noDataValue = noDataValue;
        int compatibleTag = RasterAccessor.findCompatibleTag(null, maskImage.getSampleModel());
        maskRasterFormatTag = new RasterFormatTag(maskImage.getSampleModel(), compatibleTag);
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());
        int toTileX = XToTileX(destRect.x);
        int toTileY = YToTileY(destRect.y);
        Raster maskTile = maskImage.getTile(toTileX, toTileY);
        RasterAccessor m = new RasterAccessor(maskTile, destRect, maskRasterFormatTag, maskImage.getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        switch (d.getDataType()) {
            case 0: // '\0'
                computeRectByte(s,  m, d, (byte) noDataValue);
                break;

            case 1: // '\001'
            case 2: // '\002'
                computeRectShort(s,  m, d, (short) noDataValue);
                break;

            case 3: // '\003'
                computeRectInt(s,  m, d, (int) noDataValue);
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
        byte[] m  = mData[0];
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
                if (m[mPixelOffset]!=0) {
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
        byte[] m  = mData[0];
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
                if (m[mPixelOffset]!=0) {
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
        byte[] m  = mData[0];
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
                if (m[mPixelOffset]!=0) {
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
        byte[] m  = mData[0];
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
                if (m[mPixelOffset]!=0) {
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
        byte[] m  = mData[0];
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
                if (m[mPixelOffset]!=0) {
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
