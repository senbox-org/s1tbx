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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.math.MathUtils;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.operator.MinDescriptor;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.concurrent.CancellationException;

/**
 * Creates an Densityplot from two given bands.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class DensityPlot {

    /**
     * Creates a density plot image from two raster data nodes.
     *
     * @param raster1     the first raster data node
     * @param sampleMin1  the minimum sample value to be considered in the first raster
     * @param sampleMax1  the maximum sample value to be considered in the first raster
     * @param raster2     the second raster data node
     * @param sampleMin2  the minimum sample value to be considered in the second raster
     * @param sampleMax2  the maximum sample value to be considered in the second raster
     * @param roiMask     an optional mask to be used as a ROI for the computation
     * @param width       the width of the output image
     * @param height      the height of the output image
     * @param pixelValues an which will hold the data
     * @param pm          a progress monitor
     */
    public static void accumulate(final RasterDataNode raster1, final double sampleMin1, final double sampleMax1,
                                  final RasterDataNode raster2, final double sampleMin2, final double sampleMax2,
                                  final Mask roiMask, final int width, final int height,
                                  final byte[] pixelValues, final ProgressMonitor pm) {
        Assert.notNull(raster1, "raster1");
        Assert.notNull(raster2, "raster2");
        Assert.notNull(pm, "pm");

        DensityPlotOp densityPlotOp = new DensityPlotOp(sampleMin1,
                                                        sampleMax1,
                                                        sampleMin2,
                                                        sampleMax2,
                                                        width, height);
        Shape maskShape = null;
        RenderedImage maskImage = null;
        if (roiMask != null) {
            maskShape = roiMask.getValidShape();
            maskImage = roiMask.getSourceImage();
        }
        densityPlotOp.accumulate(raster1, raster2, maskImage, maskShape, pixelValues, pm);
    }

    private static class DensityPlotOp {
        private final double sampleMin1;
        private final double sampleMax1;
        private final double sampleMin2;
        private final double sampleMax2;
        private final int width;
        private final int height;
        private final double xScale;
        private final double yScale;

        public DensityPlotOp(double sampleMin1, double sampleMax1, double sampleMin2, double sampleMax2, int width,
                             int height) {
            this.sampleMin1 = sampleMin1;
            this.sampleMax1 = sampleMax1;
            this.sampleMin2 = sampleMin2;
            this.sampleMax2 = sampleMax2;
            this.width = width;
            this.height = height;
            xScale = width / (sampleMax1 - sampleMin1);
            yScale = height / (sampleMax2 - sampleMin2);
        }

        private static void checkSampleModelForOneBand(final SampleModel sampleModel) {
            if (sampleModel.getNumBands() != 1) {
                throw new IllegalStateException("sampleModel.numBands != 1");
            }
        }

        private static void checkSampleModelForTypeByte(final SampleModel sampleModel) {
            if (sampleModel.getDataType() != DataBuffer.TYPE_BYTE) {
                throw new IllegalStateException("sampleModel.dataType != TYPE_BYTE");
            }
        }

        public void accumulate(RasterDataNode raster1, RasterDataNode raster2, RenderedImage roiImage, Shape roiShape, byte[] pixelValues, ProgressMonitor pm) {

            PlanarImage dataImage = raster1.getGeophysicalImage();
            RenderedImage dataImage1 = raster1.getGeophysicalImage();
            RenderedImage dataImage2 = raster2.getGeophysicalImage();
            final SampleModel dataSampleModel1 = dataImage1.getSampleModel();
            checkSampleModelForOneBand(dataSampleModel1);
            final SampleModel dataSampleModel2 = dataImage2.getSampleModel();
            checkSampleModelForOneBand(dataSampleModel2);

            final int dataType1 = dataSampleModel1.getDataType();
            final int dataType2 = dataSampleModel2.getDataType();
            final int maxDataType = Math.max(dataType1, dataType2);

            if (dataType1 != maxDataType) {
                dataImage1 = ImageManager.createFormatOp(dataImage1, maxDataType);
            }
            if (dataType2 != maxDataType) {
                dataImage2 = ImageManager.createFormatOp(dataImage2, maxDataType);
            }
            PixelAccessor dataAccessor1 = new PixelAccessor(dataImage1.getSampleModel(), null);
            PixelAccessor dataAccessor2 = new PixelAccessor(dataImage2.getSampleModel(), null);

            RenderedImage maskImage = raster1.getValidMaskImage();
            RenderedImage maskImage2 = raster2.getValidMaskImage();
            if (maskImage != null) {
                if (maskImage2 != null && !(maskImage == maskImage2)) {
                    maskImage = MinDescriptor.create(maskImage, maskImage2, null);
                }
                if (roiImage != null) {
                    maskImage = MinDescriptor.create(maskImage, roiImage, null);
                }
            } else {
                if (maskImage2 != null) {
                    maskImage = maskImage2;
                }
                if (maskImage != null) {
                    if (roiImage != null) {
                        maskImage = MinDescriptor.create(maskImage, roiImage, null);
                    }
                } else if (roiImage != null) {
                    maskImage = roiImage;
                }
            }
            Shape validShape1 = raster1.getValidShape();
            Shape validShape2 = raster2.getValidShape();
            Shape effectiveShape = validShape1;
            if (validShape1 != null && validShape2 != null) {
                Area area = new Area(validShape1);
                area.intersect(new Area(validShape2));
                effectiveShape = area;
            } else if (validShape2 != null) {
                effectiveShape = validShape2;
            }
            if (effectiveShape != null && roiShape != null) {
                Area area = new Area(effectiveShape);
                area.intersect(new Area(roiShape));
                effectiveShape = area;
            } else if (roiShape != null) {
                effectiveShape = roiShape;
            }

            PixelAccessor maskAccessor;
            if (maskImage != null) {
                SampleModel maskSampleModel = maskImage.getSampleModel();
                checkSampleModelForOneBand(maskSampleModel);
                checkSampleModelForTypeByte(maskSampleModel);
                maskAccessor = new PixelAccessor(maskSampleModel, null);
                // todo - assert dataImage x0,y0,w,h properties equal those of
                // maskImage (nf)
            } else {
                maskAccessor = null;
            }

            final int numXTiles = dataImage1.getNumXTiles();
            final int numYTiles = dataImage1.getNumYTiles();

            final int tileX1 = dataImage1.getTileGridXOffset();
            final int tileY1 = dataImage1.getTileGridYOffset();
            final int tileX2 = tileX1 + numXTiles - 1;
            final int tileY2 = tileY1 + numYTiles - 1;

            // todo - [multisize_products] fix: don't rely on tiling is same for dataImage1, dataImage2, maskImage (nf)
            Rectangle imageRect = new Rectangle(dataImage1.getMinX(), dataImage1.getMinY(),
                                                dataImage1.getWidth(), dataImage1.getHeight());
            try {
                pm.beginTask("Computing density plot", numXTiles * numYTiles);
                for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                    for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                        if (pm.isCanceled()) {
                            throw new CancellationException("Process terminated by user."); /* I18N */
                        }
                        boolean tileContainsData = true;
                        if (effectiveShape != null) {
                            Rectangle dataRect = dataImage.getTileRect(tileX, tileY);
                            if (!effectiveShape.intersects(dataRect)) {
                                tileContainsData = false;
                            }
                        }
                        if (tileContainsData) {
                            final Raster dataTile1 = dataImage1.getTile(tileX, tileY);
                            final Raster dataTile2 = dataImage2.getTile(tileX, tileY);
                            final Raster maskTile = maskImage != null ? maskImage.getTile(tileX, tileY) : null;
                            final Rectangle r = imageRect.intersection(dataTile1.getBounds());

                            switch (dataAccessor1.sampleType) {
                                case PixelAccessor.TYPE_BIT:
                                case DataBuffer.TYPE_BYTE:
                                    accumulateDataUByte(dataTile1, dataAccessor1,
                                                        dataTile2, dataAccessor2,
                                                        maskTile, maskAccessor, r, pixelValues);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    accumulateDataUShort(dataTile1, dataAccessor1,
                                                         dataTile2, dataAccessor2,
                                                         maskTile, maskAccessor, r, pixelValues);
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    accumulateDataShort(dataTile1, dataAccessor1,
                                                        dataTile2, dataAccessor2,
                                                        maskTile, maskAccessor, r, pixelValues);
                                    break;
                                case DataBuffer.TYPE_INT:
                                    accumulateDataInt(dataTile1, dataAccessor1,
                                                      dataTile2, dataAccessor2,
                                                      maskTile, maskAccessor, r, pixelValues);
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    accumulateDataFloat(dataTile1, dataAccessor1,
                                                        dataTile2, dataAccessor2,
                                                        maskTile, maskAccessor, r, pixelValues);
                                    break;
                                case DataBuffer.TYPE_DOUBLE:
                                    accumulateDataDouble(dataTile1, dataAccessor1,
                                                         dataTile2, dataAccessor2,
                                                         maskTile, maskAccessor, r, pixelValues);
                                    break;
                            }
                            pm.worked(1);
                        }
                    }
                }
            } finally {
                pm.done();
            }
        }

        private void accumulateDataUByte(Raster dataTile1, PixelAccessor dataAccessor1,
                                         Raster dataTile2, PixelAccessor dataAccessor2,
                                         Raster maskTile, PixelAccessor maskAccessor,
                                         Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_BYTE, false);
            final byte[] data1 = duid1.getByteData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_BYTE, false);
            final byte[] data2 = duid2.getByteData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset1] != 0)) {
                        double sample1 = data1[dataPixelOffset1] & 0xff;
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2] & 0xff;
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset1 += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }

        private void accumulateDataUShort(Raster dataTile1, PixelAccessor dataAccessor1,
                                          Raster dataTile2, PixelAccessor dataAccessor2,
                                          Raster maskTile, PixelAccessor maskAccessor,
                                          Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_USHORT, false);
            final short[] data1 = duid1.getShortData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_USHORT, false);
            final short[] data2 = duid2.getShortData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0)) {
                        double sample1 = data1[dataPixelOffset1] & 0xffff;
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2] & 0xffff;
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }

        private void accumulateDataShort(Raster dataTile1, PixelAccessor dataAccessor1,
                                         Raster dataTile2, PixelAccessor dataAccessor2,
                                         Raster maskTile, PixelAccessor maskAccessor,
                                         Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_SHORT, false);
            final short[] data1 = duid1.getShortData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_SHORT, false);
            final short[] data2 = duid2.getShortData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0)) {
                        double sample1 = data1[dataPixelOffset1];
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2];
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }

        private void accumulateDataInt(Raster dataTile1, PixelAccessor dataAccessor1,
                                       Raster dataTile2, PixelAccessor dataAccessor2,
                                       Raster maskTile, PixelAccessor maskAccessor,
                                       Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_INT, false);
            final int[] data1 = duid1.getIntData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_INT, false);
            final int[] data2 = duid2.getIntData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0)) {
                        double sample1 = data1[dataPixelOffset1];
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2];
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }

        private void accumulateDataFloat(Raster dataTile1, PixelAccessor dataAccessor1,
                                         Raster dataTile2, PixelAccessor dataAccessor2,
                                         Raster maskTile, PixelAccessor maskAccessor,
                                         Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_FLOAT, false);
            final float[] data1 = duid1.getFloatData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_FLOAT, false);
            final float[] data2 = duid2.getFloatData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0)) {
                        double sample1 = data1[dataPixelOffset1];
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2];
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }

        private void accumulateDataDouble(Raster dataTile1, PixelAccessor dataAccessor1,
                                          Raster dataTile2, PixelAccessor dataAccessor2,
                                          Raster maskTile, PixelAccessor maskAccessor,
                                          Rectangle r, byte[] pixelValues) {

            final UnpackedImageData duid1 = dataAccessor1.getPixels(dataTile1, r, DataBuffer.TYPE_DOUBLE, false);
            final double[] data1 = duid1.getDoubleData(0);
            final int dataPixelStride1 = duid1.pixelStride;
            final int dataLineStride1 = duid1.lineStride;
            final int dataBandOffset1 = duid1.bandOffsets[0];

            final UnpackedImageData duid2 = dataAccessor2.getPixels(dataTile2, r, DataBuffer.TYPE_DOUBLE, false);
            final double[] data2 = duid2.getDoubleData(0);
            final int dataPixelStride2 = duid2.pixelStride;
            final int dataLineStride2 = duid2.lineStride;
            final int dataBandOffset2 = duid2.bandOffsets[0];

            byte[] mask = null;
            int maskPixelStride = 0;
            int maskLineStride = 0;
            int maskBandOffset = 0;
            if (maskTile != null) {
                UnpackedImageData muid = maskAccessor.getPixels(maskTile, r, DataBuffer.TYPE_BYTE, false);
                mask = muid.getByteData(0);
                maskPixelStride = muid.pixelStride;
                maskLineStride = muid.lineStride;
                maskBandOffset = muid.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset = maskBandOffset;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset = maskLineOffset;
                for (int x = 0; x < r.width; x++) {
                    if ((mask == null || mask[maskPixelOffset] != 0)) {
                        double sample1 = data1[dataPixelOffset1];
                        if (sample1 >= sampleMin1 && sample1 <= sampleMax1) {
                            double sample2 = data2[dataPixelOffset2];
                            if (sample2 >= sampleMin2 && sample2 <= sampleMax2) {
                                int pixelX = MathUtils.floorInt(xScale * (sample1 - sampleMin1));
                                int pixelY = height - 1 - MathUtils.floorInt(yScale * (sample2 - sampleMin2));
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixelIndex = pixelX + pixelY * width;
                                    int pixelValue = (pixelValues[pixelIndex] & 0xff);
                                    pixelValue++;
                                    if (pixelValue > 255) {
                                        pixelValue = 255;
                                    }
                                    pixelValues[pixelIndex] = (byte) pixelValue;
                                }
                            }
                        }
                    }
                    dataPixelOffset1 += dataPixelStride1;
                    dataPixelOffset2 += dataPixelStride2;
                    maskPixelOffset += maskPixelStride;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset += maskLineStride;
            }
        }
    }
}
