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
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;

/**
 * An image that renders the data of a netcdf variable. Using the
 * "stride" feature to allow for faster subsetting.
 */
public class NetcdfOpImage extends SingleBandedOpImage {

    private final Variable variable;
    private final boolean flipY;
    private final int sourceHeight;
    private final int[] imageOrigin;
    private final Object readLock;
    private final ArrayConverter arrayConverter;
    private final int xIndex;
    private final int yIndex;
    private final int startIndexToCopy;
    private int halfSourceWidth;


    public static RenderedImage createLsbImage(Variable variable, int[] imageOrigin, boolean flipY,
                                               Object readLock, int dataBufferType,
                                               int sourceWidth, int sourceHeight,
                                               Dimension tileSize, ResolutionLevel level) {
        return new NetcdfOpImage(variable, imageOrigin, flipY, readLock, dataBufferType, sourceWidth, sourceHeight,
                                 tileSize, level, ArrayConverter.LSB);
    }

    public static RenderedImage createMsbImage(Variable variable, int[] imageOrigin, boolean flipY,
                                               Object readLock, int dataBufferType,
                                               int sourceWidth, int sourceHeight,
                                               Dimension tileSize, ResolutionLevel level) {
        return new NetcdfOpImage(variable, imageOrigin, flipY, readLock, dataBufferType, sourceWidth, sourceHeight,
                                 tileSize, level, ArrayConverter.MSB);
    }

    /**
     * Used to construct an image.
     *
     * @param variable       The netCDF variable
     * @param imageOrigin    The index within a multidimensional image dataset
     * @param flipY          The {@code true} if this data should be flipped along the yAxis.
     * @param readLock       The the lock used for reading, usually the netcdf file that contains the variable
     * @param dataBufferType The data type.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param level          The resolution level.
     */
    public NetcdfOpImage(Variable variable, int[] imageOrigin, boolean flipY, Object readLock, int dataBufferType,
                         int sourceWidth, int sourceHeight,
                         Dimension tileSize, ResolutionLevel level) {
        this(variable, imageOrigin, flipY, readLock, dataBufferType, sourceWidth, sourceHeight, tileSize, level,
             ArrayConverter.IDENTITY);
    }

    private NetcdfOpImage(Variable variable, int[] imageOrigin, boolean flipY, Object readLock, int dataBufferType,
                          int sourceWidth, int sourceHeight,
                          Dimension tileSize, ResolutionLevel level, ArrayConverter arrayConverter) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
        this.imageOrigin = imageOrigin.clone();
        this.readLock = readLock;
        this.flipY = flipY;
        this.halfSourceWidth = sourceWidth / 2;
        this.sourceHeight = sourceHeight;
        this.arrayConverter = arrayConverter;

        List<ucar.nc2.Dimension> variableDimensions = variable.getDimensions();
        DimKey rasterDim = new DimKey(variableDimensions.toArray(new ucar.nc2.Dimension[variableDimensions.size()]));
        xIndex = rasterDim.findXDimensionIndex();
        yIndex = rasterDim.findYDimensionIndex();
        startIndexToCopy = DimKey.findStartIndexOfBandVariables(variableDimensions);
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
        shape[yIndex] = sourceRect.height;
        shape[xIndex] = sourceRect.width;
        if (imageOrigin.length >= 0) {
            // todo: we need something for weird position of lat/lon in nc variables (e.g. bands data1, data2, lat, data3, lon, data4)
            System.arraycopy(imageOrigin, 0, origin, startIndexToCopy, imageOrigin.length);
        }
        origin[yIndex] = flipY ? sourceHeight - sourceRect.y - sourceRect.height : sourceRect.y;
        origin[xIndex] = sourceRect.x;
        if (isGlobalShifted180()) {
            // special case!!
            if (sourceRect.x < halfSourceWidth && sourceRect.x + sourceRect.width > halfSourceWidth) {
                computeRect180degShifted(tile, sourceRect, destRect);
                return;
            } else {
                if (sourceRect.x < halfSourceWidth) {
                    origin[xIndex] = sourceRect.x + halfSourceWidth;
                } else {
                    origin[xIndex] = sourceRect.x - halfSourceWidth;
                }
            }
        }
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
        if (xIndex < yIndex) {
            array = array.transpose(xIndex, yIndex);
        }
        // todo: consider weird position of lat/lon in nc variables (e.g. bands data1, data2, lat, data3, lon, data4), see above

        final Array convertedArray = arrayConverter.convert(array);
        if (flipY) {
            tile.setDataElements(destRect.x, destRect.y,
                                 destRect.width, destRect.height,
                                 convertedArray.flip(yIndex).copyTo1DJavaArray());
        } else {
            Object data;
            if (xIndex < yIndex) {
                data = convertedArray.copyTo1DJavaArray();
            } else {
                data = convertedArray.getStorage();
            }

            tile.setDataElements(destRect.x, destRect.y,
                                 destRect.width, destRect.height,
                                 data);
        }
    }

    private boolean isGlobalShifted180() {
        for (Attribute attribute : variable.getAttributes()) {
            // for the special case of a global image shifted by 180deg longitude, this attribute was added in CfGeocodingPart
            if (attribute.getShortName().equals("LONGITUDE_SHIFTED_180")) {
                return true;
            }
        }
        return false;
    }

    private void computeRect180degShifted(WritableRaster tile, Rectangle sourceRect, Rectangle destRect) {
        final int rank = variable.getRank();
        final int[] originLeft = new int[rank];
        final int[] originRight = new int[rank];
        final int[] shapeLeft = new int[rank];
        final int[] shapeRight = new int[rank];
        final int[] stride = new int[rank];
        for (int i = 0; i < rank; i++) {
            shapeLeft[i] = 1;
            shapeRight[i] = 1;
            originLeft[i] = 0;
            originRight[i] = 0;
            stride[i] = 1;
        }

        shapeLeft[yIndex] = sourceRect.height;
        shapeRight[yIndex] = sourceRect.height;
        shapeRight[xIndex] = halfSourceWidth - sourceRect.x;
        shapeLeft[xIndex] = sourceRect.width - shapeRight[xIndex];

        if (imageOrigin.length >= 0) {
            // todo: we need something for weird position of lat/lon in nc variables (e.g. bands data1, data2, lat, data3, lon, data4)
            System.arraycopy(imageOrigin, 0, originLeft, startIndexToCopy, imageOrigin.length);
            System.arraycopy(imageOrigin, 0, originRight, startIndexToCopy, imageOrigin.length);
        }

        originLeft[yIndex] = flipY ? sourceHeight - sourceRect.y - sourceRect.height : sourceRect.y;
        originRight[yIndex] = flipY ? sourceHeight - sourceRect.y - sourceRect.height : sourceRect.y;
        originLeft[xIndex] = 0;
        originRight[xIndex] = sourceRect.x + halfSourceWidth;

        double scale = getScale();
        stride[yIndex] = (int) scale;
        stride[xIndex] = (int) scale;
        final int halfDestWidth = (int) (halfSourceWidth / scale);

        Array arrayLeft;
        Array arrayRight;
        synchronized (readLock) {
            try {
                final Section sectionLeft = new Section(originLeft, shapeLeft, stride);
                final Section sectionRight = new Section(originRight, shapeRight, stride);
                arrayLeft = variable.read(sectionLeft);
                arrayRight = variable.read(sectionRight);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InvalidRangeException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (xIndex < yIndex) {
            arrayLeft = arrayLeft.transpose(xIndex, yIndex);
            arrayRight = arrayRight.transpose(xIndex, yIndex);
        }

        final Array convertedArrayLeft = arrayConverter.convert(arrayLeft);
        final Array convertedArrayRight = arrayConverter.convert(arrayRight);
        if (flipY) {
            tile.setDataElements(destRect.x, destRect.y,
                                 halfDestWidth - destRect.x, destRect.height,
                                 convertedArrayRight.flip(yIndex).copyTo1DJavaArray());
            tile.setDataElements(halfDestWidth, destRect.y,
                                 destRect.width - (halfDestWidth - destRect.x),
                                 destRect.height,
                                 convertedArrayLeft.flip(yIndex).copyTo1DJavaArray());
        } else {
            Object dataLeft;
            Object dataRight;
            if (xIndex < yIndex) {
                dataLeft = convertedArrayLeft.copyTo1DJavaArray();
                dataRight = convertedArrayRight.copyTo1DJavaArray();
            } else {
                dataLeft = convertedArrayLeft.getStorage();
                dataRight = convertedArrayRight.getStorage();
            }

            tile.setDataElements(destRect.x, destRect.y,
                                 halfDestWidth - destRect.x, destRect.height,
                                 dataRight);
            tile.setDataElements(halfDestWidth, destRect.y,
                                 destRect.width - (halfDestWidth - destRect.x), destRect.height,
                                 dataLeft);
        }
    }

    private interface ArrayConverter {

        public ArrayConverter IDENTITY = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                return array;
            }
        };

        public ArrayConverter LSB = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                final Array convertedArray = Array.factory(DataType.INT, array.getShape());
                for (int i = 0; i < convertedArray.getSize(); i++) {
                    convertedArray.setInt(i, (int) (array.getLong(i) & 0x00000000FFFFFFFFL));
                }
                return convertedArray;
            }
        };

        public ArrayConverter MSB = new ArrayConverter() {
            @Override
            public Array convert(Array array) {
                final Array convertedArray = Array.factory(DataType.INT, array.getShape());
                for (int i = 0; i < convertedArray.getSize(); i++) {
                    convertedArray.setInt(i, (int) (array.getLong(i) >>> 32));
                }
                return convertedArray;
            }
        };

        Array convert(Array array);
    }

    private Rectangle getSourceRect(Rectangle rect) {
        int sourceX = getSourceX(rect.x);
        int sourceY = getSourceY(rect.y);
        int sourceWidth = getSourceWidth(rect.width);
        int sourceHeight = getSourceHeight(rect.height);
        return new Rectangle(sourceX, sourceY, sourceWidth, sourceHeight);
    }
}
