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
package org.esa.snap.core.gpf.common.reproject;

import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

final class Log10OpImage extends PointOpImage {


    Log10OpImage(RenderedImage source) {
        super(source, null, null, true);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());
        switch (d.getDataType()) {
            case 4: // '\004'
                computeRectFloat(s, d);
                break;
            case 5: // '\005'
                computeRectDouble(s, d);
                break;
        }
        d.copyDataToRaster();
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor dst) {
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
                float sourceValue = s[sPixelOffset];
                if (Float.isNaN(sourceValue)) {
                    d[dPixelOffset] = sourceValue;
                } else {
                    d[dPixelOffset] = (float) Math.log10(sourceValue);
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor dst) {
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
                double sourceValue = s[sPixelOffset];
                if (Double.isNaN(sourceValue)) {
                    d[dPixelOffset] = sourceValue;
                } else {
                    d[dPixelOffset] = Math.log10(sourceValue);
                }
                sPixelOffset += sPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }
}
