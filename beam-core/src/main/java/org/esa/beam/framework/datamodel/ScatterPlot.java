/*
 * $Id: $
 * 
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.concurrent.CancellationException;

import javax.media.jai.PixelAccessor;
import javax.media.jai.ROI;
import javax.media.jai.UnpackedImageData;

import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

/**
 * Creates an Scatterplot from two given bands.
 * 
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class ScatterPlot {

    /**
     * Creates a scatter plot image from two raster data nodes.
     * 
     * @param raster1
     *            the first raster data node
     * @param sampleMin1
     *            the minimum sample value to be considered in the first raster
     * @param sampleMax1
     *            the maximum sample value to be considered in the first raster
     * @param raster2
     *            the second raster data node
     * @param sampleMin2
     *            the minimum sample value to be considered in the second raster
     * @param sampleMax2
     *            the maximum sample value to be considered in the second raster
     * @param roi
     *            an optional ROI to be used for the computation
     * @param width
     *            the width of the output image
     * @param height
     *            the height of the output image
     * @param level
     *            the level at which the scatterplot should be computed
     * @param pixelValues
     *            an which will hold the data
     */
    public static void accumulate(final RasterDataNode raster1, final double sampleMin1, final double sampleMax1,
                                  final RasterDataNode raster2, final double sampleMin2, final double sampleMax2,
                                  final ROI roi, final int width, final int height, int level,
                                  final byte[] pixelValues, final ProgressMonitor pm) {
        Assert.notNull(raster1, "raster1");
        Assert.notNull(raster2, "raster2");
        Assert.notNull(pm, "pm");

        ScatterPlotOp scatterPlotOp = new ScatterPlotOp(raster1.scaleInverse(sampleMin1), 
                                                        raster1.scaleInverse(sampleMax1), 
                                                        raster2.scaleInverse(sampleMin2),
                                                        raster2.scaleInverse(sampleMax2),
                                                        width, height);
        scatterPlotOp.accumulate(raster1, raster2, roi, level, pixelValues, pm);
    }

    private static class ScatterPlotOp {
        private final double sampleMin1;
        private final double sampleMax1;
        private final double sampleMin2;
        private final double sampleMax2;
        private final int width;
        private final int height;
        private final double xScale;
        private final double yScale;
        private RenderedImage dataImage1;
        private RenderedImage dataImage2;
        private RenderedImage maskImage1;
        private PixelAccessor maskAccessor1;
        private RenderedImage maskImage2;
        private PixelAccessor maskAccessor2;
        private PixelAccessor dataAccessor1;
        private PixelAccessor dataAccessor2;

        public ScatterPlotOp(double sampleMin1, double sampleMax1, double sampleMin2, double sampleMax2, int width,
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

        public void accumulate(RasterDataNode raster1, RasterDataNode raster2, ROI roi, int level, byte[] pixelValues,
                               ProgressMonitor pm) {

            preprareDataImages(raster1, raster2, level);
            prepareMaskImages(raster1, raster2, level);

            accumulate(roi, pixelValues, pm);
        }

        private void prepareMaskImages(RasterDataNode raster1, RasterDataNode raster2, int level) {
            maskImage1 = ImageManager.getInstance().getValidMaskImage(raster1, level);
            if (maskImage1 != null) {
                SampleModel maskSampleModel = maskImage1.getSampleModel();
                checkSampleModelForOneBand(maskSampleModel);
                checkSampleModelForTypeByte(maskSampleModel);
                maskAccessor1 = new PixelAccessor(maskSampleModel, null);
                // todo - assert dataImage x0,y0,w,h properties equal those of
                // maskImage (nf)
            } else {
                maskAccessor1 = null;
            }

            maskImage2 = ImageManager.getInstance().getValidMaskImage(raster2, level);
            if (maskImage2 != null) {
                SampleModel maskSampleModel = maskImage2.getSampleModel();
                checkSampleModelForOneBand(maskSampleModel);
                checkSampleModelForTypeByte(maskSampleModel);
                maskAccessor2 = new PixelAccessor(maskSampleModel, null);
                // todo - assert dataImage x0,y0,w,h properties equal those of
                // maskImage (nf)
            } else {
                maskAccessor2 = null;
            }
        }

        private void preprareDataImages(RasterDataNode raster1, RasterDataNode raster2, int level) {
            dataImage1 = ImageManager.getInstance().getSourceImage(raster1, level);
            dataImage2 = ImageManager.getInstance().getSourceImage(raster2, level);
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
            dataAccessor1 = new PixelAccessor(dataSampleModel1, null);
            dataAccessor2 = new PixelAccessor(dataSampleModel2, null);

        }

        private void accumulate(final ROI roi, final byte[] pixelValues, final ProgressMonitor pm) {

            final int numXTiles = dataImage1.getNumXTiles();
            final int numYTiles = dataImage1.getNumYTiles();

            final int tileX1 = dataImage1.getTileGridXOffset();
            final int tileY1 = dataImage1.getTileGridYOffset();
            final int tileX2 = tileX1 + numXTiles - 1;
            final int tileY2 = tileY1 + numYTiles - 1;

            // todo - assert dataImage tile properties equal those of maskImage
            // (nf)

            try {
                pm.beginTask("Computing scatterplot", numXTiles * numYTiles);
                for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                    for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                        if (pm.isCanceled()) {
                            throw new CancellationException("Process terminated by user."); /* I18N */
                        }
                        final Raster dataTile1 = dataImage1.getTile(tileX, tileY);
                        final Raster dataTile2 = dataImage2.getTile(tileX, tileY);
                        final Raster maskTile1 = maskImage1 != null ? maskImage1.getTile(tileX, tileY) : null;
                        final Raster maskTile2 = maskImage2 != null ? maskImage2.getTile(tileX, tileY) : null;
                        final Rectangle r = new Rectangle(dataImage1.getMinX(), dataImage1.getMinY(),
                                                          dataImage1.getWidth(), dataImage1.getHeight())
                                                                                                        .intersection(dataTile1
                                                                                                                               .getBounds());
                        switch (dataAccessor1.sampleType) {
                            case PixelAccessor.TYPE_BIT:
                            case DataBuffer.TYPE_BYTE:
                                accumulateDataUByte(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                accumulateDataUShort(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                            case DataBuffer.TYPE_SHORT:
                                accumulateDataShort(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                            case DataBuffer.TYPE_INT:
                                accumulateDataInt(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                accumulateDataFloat(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                accumulateDataDouble(dataTile1, maskTile1, dataTile2, maskTile2, r, roi, pixelValues);
                                break;
                        }
                        pm.worked(1);
                    }
                }
            } finally {
                pm.done();
            }
        }

        private void accumulateDataUByte(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                         Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }

        private void accumulateDataUShort(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                          Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }

        private void accumulateDataShort(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                         Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }

        private void accumulateDataInt(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                       Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }

        private void accumulateDataFloat(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                         Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }

            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }

        private void accumulateDataDouble(Raster dataTile1, Raster maskTile1, Raster dataTile2, Raster maskTile2,
                                          Rectangle r, ROI roi, byte[] pixelValues) {

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

            byte[] mask1 = null;
            int maskPixelStride1 = 0;
            int maskLineStride1 = 0;
            int maskBandOffset1 = 0;
            if (maskTile1 != null) {
                UnpackedImageData muid1 = maskAccessor1.getPixels(maskTile1, r, DataBuffer.TYPE_BYTE, false);
                mask1 = muid1.getByteData(0);
                maskPixelStride1 = muid1.pixelStride;
                maskLineStride1 = muid1.lineStride;
                maskBandOffset1 = muid1.bandOffsets[0];
            }

            byte[] mask2 = null;
            int maskPixelStride2 = 0;
            int maskLineStride2 = 0;
            int maskBandOffset2 = 0;
            if (maskTile2 != null) {
                UnpackedImageData muid2 = maskAccessor2.getPixels(maskTile2, r, DataBuffer.TYPE_BYTE, false);
                mask2 = muid2.getByteData(0);
                maskPixelStride2 = muid2.pixelStride;
                maskLineStride2 = muid2.lineStride;
                maskBandOffset2 = muid2.bandOffsets[0];
            }
           
            int dataLineOffset1 = dataBandOffset1;
            int dataLineOffset2 = dataBandOffset2;
            int maskLineOffset1 = maskBandOffset1;
            int maskLineOffset2 = maskBandOffset2;
            for (int y = 0; y < r.height; y++) {
                int dataPixelOffset1 = dataLineOffset1;
                int dataPixelOffset2 = dataLineOffset2;
                int maskPixelOffset1 = maskLineOffset1;
                int maskPixelOffset2 = maskLineOffset2;
                for (int x = 0; x < r.width; x++) {
                    if ((mask1 == null || mask1[maskPixelOffset1] != 0)
                            && (mask2 == null || mask2[maskPixelOffset2] != 0)
                            && (roi == null || roi.contains(r.x + x, r.y + y))) {
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
                    maskPixelOffset1 += maskPixelStride1;
                    maskPixelOffset2 += maskPixelStride2;
                }
                dataLineOffset1 += dataLineStride1;
                dataLineOffset2 += dataLineStride2;
                maskLineOffset1 += maskLineStride1;
                maskLineOffset2 += maskLineStride2;
            }
        }
    }
}
