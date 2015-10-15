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

import org.esa.snap.core.gpf.internal.OperatorContext;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.Warp;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
class WarpSourceCoordinatesOpImage extends SourcelessOpImage {

    private final Warp warp;
    private final RasterFormatTag rasterFormatTag;

    private static ImageLayout createTwoBandedImageLayout(int width, int height, Dimension tileSize) {
        if (width < 0) {
            throw new IllegalArgumentException("width");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height");
        }
        if (tileSize == null) {
            throw new IllegalArgumentException("tileSize");
        }
        SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT,
                                                                                  tileSize.width, tileSize.height, 2);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        if (colorModel == null) {
            final int dataType = sampleModel.getDataType();
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {DataBuffer.getDataTypeSize(dataType)};
            colorModel = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, dataType);
        }
        return new ImageLayout(0, 0, width, height, 0, 0, tileSize.width, tileSize.height, sampleModel, colorModel);
    }


    /**
     * @param warp
     * @param width
     * @param height
     * @param tileSize
     * @param configuration
     */
    WarpSourceCoordinatesOpImage(Warp warp, int width, int height, Dimension tileSize,
                                 Map configuration) {
        this(warp, createTwoBandedImageLayout(width, height, tileSize), configuration);
    }

    private WarpSourceCoordinatesOpImage(Warp warp, ImageLayout layout, Map configuration) {
        super(layout, configuration, layout.getSampleModel(null), layout.getMinX(null), layout.getMinY(null),
              layout.getWidth(null), layout.getHeight(null));
        this.warp = warp;
        int compatibleTag = RasterAccessor.findCompatibleTag(null, layout.getSampleModel(null));
        rasterFormatTag = new RasterFormatTag(layout.getSampleModel(null), compatibleTag);
        OperatorContext.setTileCache(this);
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
//        System.out.println("WarpSourceCoordinatesOpImage "+destRect);
//        long t1 = System.currentTimeMillis();
        RasterAccessor dst = new RasterAccessor(dest, destRect, rasterFormatTag, getColorModel());
        computeRectFloat(dst);
        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }
//        long t2 = System.currentTimeMillis();
//        System.out.println("WarpSourceCoordinatesOpImage "+(t2-t1));
    }

    private void computeRectFloat(RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        float[][] data = dst.getFloatDataArrays();
        float[] warpData = new float[2 * dstWidth * dstHeight];
        int lineOffset = 0;

        warp.warpRect(dst.getX(), dst.getY(), dstWidth, dstHeight, warpData);
        int count = 0;
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;
            for (int w = 0; w < dstWidth; w++) {
                float x = warpData[count++];
                float y = warpData[count++];
                // if x or y is NaN set them to values outside image bounds
                // if NaN is forwarded it gets casted to an integer pixel index and
                // the result would be zero and therefore a valid pixel.
                if ((Float.isNaN(x) || Float.isNaN(y))) {
                    x = (float) (getBounds().getMinX() - 1);
                    y = (float) (getBounds().getMinY() - 1);
                }
                data[0][pixelOffset + bandOffsets[0]] = x;
                data[1][pixelOffset + bandOffsets[1]] = y;
                pixelOffset += pixelStride;
            }
        }
    }
}
