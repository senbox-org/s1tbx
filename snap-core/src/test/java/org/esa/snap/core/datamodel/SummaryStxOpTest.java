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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ImageUtils;
import org.junit.Test;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import static org.junit.Assert.*;

/**
 * Date: 04.05.11
 */
public class SummaryStxOpTest {

    @Test
    public void testAccumulateDataByte() throws Exception {
        byte[] data = new byte[]{0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20};

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferByte(data, data.length)), null);

        assertEquals(0.0, op.getMinimum(), 1.0e-8);
        assertEquals(20.0, op.getMaximum(), 1.0e-8);
        assertEquals(10.0, op.getMean(), 1.0e-8);
        assertEquals(44.0, op.getVariance(), 1.0e-8);
        assertEquals(6.63324958, op.getStandardDeviation(), 1.0e-8);
    }

    @Test
    public void testAccumulateDataShort() throws Exception {
        short[] data = new short[]{13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferShort(data, data.length)), null);

        assertEquals(13.0, op.getMinimum(), 1.0e-8);
        assertEquals(23.0, op.getMaximum(), 1.0e-8);
        assertEquals(18.0, op.getMean(), 1.0e-8);
        assertEquals(11.0, op.getVariance(), 1.0e-8);
        assertEquals(3.31662479, op.getStandardDeviation(), 1.0e-8);
    }

    @Test
    public void testAccumulateDataUShort() throws Exception {
        short[] data = new short[]{
                (short) 65535, (short) 65534, (short) 65533, (short) 65532, (short) 65531,
                (short) 65530, (short) 65529, (short) 65528, (short) 65527, (short) 65526, (short) 65525
        };

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferUShort(data, data.length)), null);

        assertEquals(65525.0, op.getMinimum(), 1.0e-8);
        assertEquals(65535.0, op.getMaximum(), 1.0e-8);
        assertEquals(65530.0, op.getMean(), 1.0e-8);
        assertEquals(11.0, op.getVariance(), 1.0e-8);
        assertEquals(3.31662479, op.getStandardDeviation(), 1.0e-8);
    }

    @Test
    public void testAccumulateDataInt() throws Exception {
        int[] data = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferInt(data, data.length)), null);

        assertEquals(0.0, op.getMinimum(), 1.0e-8);
        assertEquals(10.0, op.getMaximum(), 1.0e-8);
        assertEquals(5.0, op.getMean(), 1.0e-8);
        assertEquals(11.0, op.getVariance(), 1.0e-8);
        assertEquals(3.31662479, op.getStandardDeviation(), 1.0e-8);
    }

    @Test
    public void testAccumulateDataFloat() throws Exception {
        float[] data = new float[]{18.6f, 18.5f, 18.4f, 18.3f, 18.2f, 18.1f, 18.0f, 17.9f, 17.8f, 17.7f, 17.6f};

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferFloat(data, data.length)), null);

        assertEquals(17.6, op.getMinimum(), 1.0e-6);
        assertEquals(18.6, op.getMaximum(), 1.0e-6);
        assertEquals(18.1, op.getMean(), 1.0e-6);
        assertEquals(0.11, op.getVariance(), 1.0e-6);
        assertEquals(0.331662, op.getStandardDeviation(), 1.0e-6);
    }

    @Test
    public void testAccumulateWithOnlyNoData() throws Exception {
        float[] data = new float[11];
        RenderedOp maskImage = ConstantDescriptor.create((float) data.length, 1.0f, new Byte[]{0}, null);

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferFloat(data, data.length)), getPixels(maskImage));

        assertEquals(Double.NaN, op.getMinimum(), 1.0e-6);
        assertEquals(Double.NaN, op.getMaximum(), 1.0e-6);
        assertEquals(Double.NaN, op.getMean(), 1.0e-6);
        assertEquals(Double.NaN, op.getVariance(), 1.0e-6);
        assertEquals(Double.NaN, op.getStandardDeviation(), 1.0e-6);
    }

    @Test
    public void testAccumulateWithOnlyNaNDataWithoutMask() throws Exception {
        final RenderedOp nanImage = ConstantDescriptor.create(11.0f, 1.0f, new Float[]{Float.NaN}, null);

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(nanImage), null);

        assertEquals(Double.NaN, op.getMinimum(), 1.0e-6);
        assertEquals(Double.NaN, op.getMaximum(), 1.0e-6);
        assertEquals(Double.NaN, op.getMean(), 1.0e-6);
        assertEquals(Double.NaN, op.getVariance(), 1.0e-6);
        assertEquals(Double.NaN, op.getStandardDeviation(), 1.0e-6);
    }

    @Test
    public void testAccumulateDataDouble() throws Exception {
        double[] data = new double[]{18.6, 18.7, 18.8, 18.9, 19.0, 19.1, 19.2, 19.3, 19.4, 19.5, 19.6};

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(new DataBufferDouble(data, data.length)), null);

        assertEquals(18.6, op.getMinimum(), 1.0e-8);
        assertEquals(19.6, op.getMaximum(), 1.0e-8);
        assertEquals(19.1, op.getMean(), 1.0e-8);
        assertEquals(0.11, op.getVariance(), 1.0e-8);
        assertEquals(0.33166247, op.getStandardDeviation(), 1.0e-8);
    }

    @Test
    public void testAccumulateDataDoubleWithNegativeValues() throws Exception {
        double[] data = new double[]{-1.6, -1.7, -1.8, -1.9, -2.0, -2.1, -2.2, -2.3, -2.4, -2.5, -2.6};
        RenderedImage image = new BufferedOpImage(new DataBufferDouble(data, data.length));

        SummaryStxOp op = new SummaryStxOp();
        op.accumulateData(getPixels(image), null);

        assertEquals(-2.6, op.getMinimum(), 1.0e-8);
        assertEquals(-1.6, op.getMaximum(), 1.0e-8);
        assertEquals(-2.1, op.getMean(), 1.0e-8);
        assertEquals(0.11, op.getVariance(), 1.0e-8);
        assertEquals(0.33166247, op.getStandardDeviation(), 1.0e-8);
    }

    private UnpackedImageData getPixels(DataBuffer dataBuffer) {
        return getPixels(new BufferedOpImage(dataBuffer));
    }

    private UnpackedImageData getPixels(RenderedImage image) {
        PixelAccessor dataAccessor = new PixelAccessor(image);
        return dataAccessor.getPixels(image.getData(),
                                      image.getData().getBounds(),
                                      image.getSampleModel().getDataType(), false);
    }

    private static class BufferedOpImage extends SourcelessOpImage {

        private DataBuffer dataBuffer;

        BufferedOpImage(DataBuffer dataBuffer) {
            super(ImageManager.createSingleBandedImageLayout(dataBuffer.getDataType(),
                                                             dataBuffer.getSize(), 1,
                                                             dataBuffer.getSize(), 1),
                  null,
                  ImageUtils.createSingleBandedSampleModel(dataBuffer.getDataType(), dataBuffer.getSize(), 1),
                  0, 0, dataBuffer.getSize(), 1);
            this.dataBuffer = dataBuffer;
        }

        @Override
        protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
            for (int y = 0; y < destRect.height; y++) {
                for (int x = 0; x < destRect.width; x++) {
                    if (dataBuffer.getDataType() == DataBuffer.TYPE_FLOAT) {
                        dest.setSample(x, y, 0, dataBuffer.getElemFloat(y * destRect.width + x));
                    } else if (dataBuffer.getDataType() == DataBuffer.TYPE_DOUBLE) {
                        dest.setSample(x, y, 0, dataBuffer.getElemDouble(y * destRect.width + x));
                    } else {
                        dest.setSample(x, y, 0, dataBuffer.getElem(y * destRect.width + x));
                    }
                }
            }

        }
    }


}







