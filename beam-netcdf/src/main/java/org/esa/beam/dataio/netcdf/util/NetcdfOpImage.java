/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * An image that renders the data of a netcdf variable. Using the
 * "stride" feature to allow for faster subsetting.
 */
public class NetcdfOpImage extends SingleBandedOpImage {

    private final Variable variable;
    private final boolean isYFlipped;
    private final int sourceHeight;
    private final int[] imageOrigin;
    private final Object readLock;

    /**
     * Used to construct an image.
     *
     * @param variable       The netCDF variable
     * @param imageOrigin    The index within a multidimensional image dataset
     * @param isYFlipped     The {@code true} if this data should be flipped along the yAxis.
     * @param readLock       The the lock used for reading, usually the netcdf file that contains the variable
     * @param dataBufferType The data type.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param level          The resolution level.
     */
    public NetcdfOpImage(Variable variable, int[] imageOrigin, boolean isYFlipped, Object readLock, int dataBufferType,
                         int sourceWidth, int sourceHeight,
                         Dimension tileSize, ResolutionLevel level) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
        this.imageOrigin = imageOrigin.clone();
        this.readLock = readLock;
        this.isYFlipped = isYFlipped;
        this.sourceHeight = sourceHeight;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        Rectangle sourceRect;
        if (getLevel() != 0) {
            sourceRect = getSourceRect(destRect);
        } else {
            sourceRect = destRect;
        }

        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        final int[] stride = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
            stride[i] = 1;
        }
        final int xIndex = rank - 1;
        final int yIndex = rank - 2;

        shape[yIndex] = sourceRect.height;
        shape[xIndex] = sourceRect.width;

        if (imageOrigin.length >= 0) {
            System.arraycopy(imageOrigin, 0, origin, 0, imageOrigin.length);
        }
        origin[yIndex] = isYFlipped ? sourceHeight - sourceRect.y - sourceRect.height : sourceRect.y;
        origin[xIndex] = sourceRect.x;

        double scale = getScale();
        stride[yIndex] = (int) scale;
        stride[xIndex] = (int) scale;

        Array array;
        synchronized (readLock) {
            try {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InvalidRangeException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (isYFlipped) {
            Array flippedArray = array.flip(yIndex);
            tile.setDataElements(destRect.x, destRect.y,
                                 destRect.width, destRect.height,
                                 flippedArray.copyTo1DJavaArray());
        } else {
            tile.setDataElements(destRect.x, destRect.y,
                                 destRect.width, destRect.height,
                                 array.getStorage());
        }
    }

    private Rectangle getSourceRect(Rectangle rect) {
        int sourceX = getSourceX(rect.x);
        int sourceY = getSourceY(rect.y);
        int sourceWidth = getSourceWidth(rect.width);
        int sourceHeight = getSourceHeight(rect.height);
        return new Rectangle(sourceX, sourceY, sourceWidth, sourceHeight);
    }
}
