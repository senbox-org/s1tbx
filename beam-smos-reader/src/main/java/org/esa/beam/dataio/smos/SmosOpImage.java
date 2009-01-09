/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.*;
import java.util.Arrays;

class SmosOpImage extends SingleBandedOpImage {

    private final GridPointValueProvider valueProvider;
    private final RasterDataNode node;
    private final RenderedImage seqnumImage;
    private final Area rectifiedRegion;

    SmosOpImage(GridPointValueProvider valueProvider, RasterDataNode node, RenderedImage seqnumImage,
                ResolutionLevel level, Area region) {
        super(ImageManager.getDataBufferType(node.getDataType()),
              node.getSceneRasterWidth(),
              node.getSceneRasterHeight(),
              node.getProduct().getPreferredTileSize(),
              null, // no configuration
              level);

        this.valueProvider = valueProvider;
        this.node = node;
        this.seqnumImage = seqnumImage;

        rectifiedRegion = new Area();
        for (int x = 0; x < getNumXTiles(); ++x) {
            for (int y = 0; y < getNumYTiles(); ++y) {
                final Rectangle tileRect = getTileRect(x, y);
                if (region.intersects(tileRect)) {
                    rectifiedRegion.add(new Area(tileRect));
                }
            }
        }
    }

    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        final double noDataValue = node.getNoDataValue();

        final int x = rectangle.x;
        final int y = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;

        if (!rectifiedRegion.intersects(x, y, w, h)) {
            switch (targetRaster.getTransferType()) {
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_USHORT:
                    targetRaster.setDataElements(x, y, w, h, createArray(w, h, (short) noDataValue));
                    return;
                case DataBuffer.TYPE_INT:
                    targetRaster.setDataElements(x, y, w, h, createArray(w, h, (int) noDataValue));
                    return;
                case DataBuffer.TYPE_FLOAT:
                    targetRaster.setDataElements(x, y, w, h, createArray(w, h, (float) noDataValue));
                    return;
                default:
                    // do nothing
                    return;
            }
        }

        final Raster seqnumRaster = seqnumImage.getData(rectangle);
        final ColorModel cm = seqnumImage.getColorModel();

        final PixelAccessor seqnumAccessor = new PixelAccessor(seqnumRaster.getSampleModel(), cm);
        final PixelAccessor targetAccessor = new PixelAccessor(targetRaster.getSampleModel(), null);

        final UnpackedImageData seqnumData = seqnumAccessor.getPixels(
                seqnumRaster, rectangle, seqnumRaster.getSampleModel().getTransferType(), false);
        final UnpackedImageData targetData = targetAccessor.getPixels(
                targetRaster, rectangle, targetRaster.getSampleModel().getTransferType(), true);

        switch (targetData.type) {
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
                shortLoop(seqnumData, targetData, (short) noDataValue);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(seqnumData, targetData, (int) noDataValue);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(seqnumData, targetData, (float) noDataValue);
                break;
            default:
                // do nothing
                break;
        }

        targetAccessor.setPixels(targetData);
    }

    private static short[] createArray(int w, int h, short value) {
        final short[] array = new short[w * h];
        Arrays.fill(array, value);

        return array;
    }

    private static int[] createArray(int w, int h, int value) {
        final int[] array = new int[w * h];
        Arrays.fill(array, value);

        return array;
    }

    private static float[] createArray(int w, int h, float value) {
        final float[] array = new float[w * h];
        Arrays.fill(array, value);

        return array;
    }

    private void shortLoop(UnpackedImageData seqnumData, UnpackedImageData targetData, short noDataValue) {
        final int w = targetData.rect.width;
        final int h = targetData.rect.height;

        final int seqnumPixelStride = seqnumData.pixelStride;
        final int seqnumLineStride = seqnumData.lineStride;
        final int[] seqnumDataArray = seqnumData.getIntData(0);

        final int targetPixelStride = targetData.pixelStride;
        final int targetLineStride = targetData.lineStride;
        final short[] targetDataArray = targetData.getShortData(0);

        final int[] gridPointIndexCache = new int[w];
        final short[] valueCache = new short[w];

        int seqnumLineOffset = seqnumData.getOffset(0);
        int targetLineOffset = targetData.getOffset(0);

        for (int y = 0; y < h; ++y) {
            int seqnumPixelOffset = seqnumLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < w; ++x) {
                final int seqnum = seqnumDataArray[seqnumPixelOffset];
                final int gridPointIndex = valueProvider.getGridPointIndex(seqnum);
                short value = noDataValue;

                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[x - 1] == gridPointIndex) {
                        // pixel to the west
                        value = valueCache[x - 1];
                    } else if (y > 0 && gridPointIndexCache[x] == gridPointIndex) {
                        // pixel to the north
                        value = valueCache[x];
                    } else if (x + 1 < w && y > 0 && gridPointIndexCache[x + 1] == gridPointIndex) {
                        // pixel to the north-east
                        value = valueCache[x + 1];
                    } else {
                        value = valueProvider.getValue(gridPointIndex, noDataValue);
                    }
                }

                gridPointIndexCache[x] = gridPointIndex;
                valueCache[x] = value;

                targetDataArray[targetPixelOffset] = value;
                seqnumPixelOffset += seqnumPixelStride;
                targetPixelOffset += targetPixelStride;
            }

            seqnumLineOffset += seqnumLineStride;
            targetLineOffset += targetLineStride;
        }
    }

    private void intLoop(UnpackedImageData seqnumData, UnpackedImageData targetData, int noDataValue) {
        final int w = targetData.rect.width;
        final int h = targetData.rect.height;

        final int seqnumPixelStride = seqnumData.pixelStride;
        final int seqnumLineStride = seqnumData.lineStride;
        final int[] seqnumDataArray = seqnumData.getIntData(0);

        final int targetPixelStride = targetData.pixelStride;
        final int targetLineStride = targetData.lineStride;
        final int[] targetDataArray = targetData.getIntData(0);

        final int[] gridPointIndexCache = new int[w];
        final int[] valueCache = new int[w];

        int seqnumLineOffset = seqnumData.getOffset(0);
        int targetLineOffset = targetData.getOffset(0);

        for (int y = 0; y < h; ++y) {
            int seqnumPixelOffset = seqnumLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < w; ++x) {
                final int seqnum = seqnumDataArray[seqnumPixelOffset];
                final int gridPointIndex = valueProvider.getGridPointIndex(seqnum);
                int value = noDataValue;

                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[x - 1] == gridPointIndex) {
                        // pixel to the west
                        value = valueCache[x - 1];
                    } else if (y > 0 && gridPointIndexCache[x] == gridPointIndex) {
                        // pixel to the north
                        value = valueCache[x];
                    } else if (x + 1 < w && y > 0 && gridPointIndexCache[x + 1] == gridPointIndex) {
                        // pixel to the north-east
                        value = valueCache[x + 1];
                    } else {
                        value = valueProvider.getValue(gridPointIndex, noDataValue);
                    }
                }
                gridPointIndexCache[x] = gridPointIndex;
                valueCache[x] = value;

                targetDataArray[targetPixelOffset] = value;
                seqnumPixelOffset += seqnumPixelStride;
                targetPixelOffset += targetPixelStride;
            }

            seqnumLineOffset += seqnumLineStride;
            targetLineOffset += targetLineStride;
        }
    }

    private void floatLoop(UnpackedImageData seqnumData, UnpackedImageData targetData, float noDataValue) {
        final int w = targetData.rect.width;
        final int h = targetData.rect.height;

        final int seqnumPixelStride = seqnumData.pixelStride;
        final int seqnumLineStride = seqnumData.lineStride;
        final int[] seqnumDataArray = seqnumData.getIntData(0);

        final int targetPixelStride = targetData.pixelStride;
        final int targetLineStride = targetData.lineStride;
        final float[] targetDataArray = targetData.getFloatData(0);

        final int[] gridPointIndexCache = new int[w];
        final float[] valueCache = new float[w];

        int seqnumLineOffset = seqnumData.getOffset(0);
        int targetLineOffset = targetData.getOffset(0);

        for (int y = 0; y < h; ++y) {
            int seqnumPixelOffset = seqnumLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < w; ++x) {
                final int seqnum = seqnumDataArray[seqnumPixelOffset];
                final int gridPointIndex = valueProvider.getGridPointIndex(seqnum);
                float value = noDataValue;

                if (gridPointIndex != -1) {
                    if (x > 0 && gridPointIndexCache[x - 1] == gridPointIndex) {
                        // pixel to the west
                        value = valueCache[x - 1];
                    } else if (y > 0 && gridPointIndexCache[x] == gridPointIndex) {
                        // pixel to the north
                        value = valueCache[x];
                    } else if (x + 1 < w && y > 0 && gridPointIndexCache[x + 1] == gridPointIndex) {
                        // pixel to the north-east
                        value = valueCache[x + 1];
                    } else {
                        value = valueProvider.getValue(gridPointIndex, noDataValue);
                    }
                }
                gridPointIndexCache[x] = gridPointIndex;
                valueCache[x] = value;

                targetDataArray[targetPixelOffset] = value;
                seqnumPixelOffset += seqnumPixelStride;
                targetPixelOffset += targetPixelStride;
            }

            seqnumLineOffset += seqnumLineStride;
            targetLineOffset += targetLineStride;
        }
    }
}
