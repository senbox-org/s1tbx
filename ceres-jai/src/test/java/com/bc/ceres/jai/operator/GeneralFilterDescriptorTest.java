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

package com.bc.ceres.jai.operator;

import com.bc.ceres.jai.GeneralFilterFunction;
import org.junit.Test;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferUShort;

import static org.junit.Assert.*;


public class GeneralFilterDescriptorTest {

    @Test
    public void testMax() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 4,
                9, 0, 1, 2, 3,
                8, 9, 0, 1, 2,
                7, 8, 9, 0, 1,
                6, 7, 8, 9, 0,
        };
        short[] expectedData = new short[]{
                9, 9, 3, 4, 4,
                9, 9, 9, 4, 4,
                9, 9, 9, 9, 3,
                9, 9, 9, 9, 9,
                8, 9, 9, 9, 9,
        };

        BufferedImage image = SourceImageFactory.createOneBandedUShortImage(5, 5, sourceData);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        GeneralFilterFunction.Max max = new GeneralFilterFunction.Max(3, 3, 1, 1, null);
        RenderedOp op = GeneralFilterDescriptor.create(image, max, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
        assertNotNull(op);
        assertEquals(5, op.getWidth());
        assertEquals(5, op.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, op.getSampleModel().getDataType());

        DataBufferUShort destBuffer = ((DataBufferUShort) op.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }


    @Test
    public void testMedian() {
        short[] sourceData = new short[]{
                0, 1, 2, 3, 4,
                9, 0, 1, 2, 3,
                8, 9, 0, 1, 2,
                7, 8, 9, 0, 1,
                6, 7, 8, 9, 0,
        };
        short[] expectedData = new short[]{
                0, 0, 1, 2, 0,
                0, 1, 1, 2, 2,
                7, 8, 1, 1, 1,
                7, 8, 8, 1, 0,
                0, 7, 7, 0, 0,
        };

        BufferedImage image = SourceImageFactory.createOneBandedUShortImage(5, 5, sourceData);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        GeneralFilterFunction.Median median = new GeneralFilterFunction.Median(3, 3, 1, 1, null);
        RenderedOp op = GeneralFilterDescriptor.create(image, median, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
        assertNotNull(op);
        assertEquals(5, op.getWidth());
        assertEquals(5, op.getHeight());
        assertEquals(DataBuffer.TYPE_USHORT, op.getSampleModel().getDataType());

        DataBufferUShort destBuffer = ((DataBufferUShort) op.getData().getDataBuffer());
        short[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i]);
        }
    }

    @Test
    public void testMedian_WithNaNs() {
        float n = Float.NaN;
        float[] sourceData = new float[]{
                0, 1, 2, n, 4,
                9, 0, 1, 2, 3,
                8, 9, 0, n, 2,
                7, n, 9, 0, 1,
                n, 7, 8, 9, 0,
        };

        float[] expectedData = new float[]{
                0.0f, 0.0f, 0.5f, 1.5f, 0.0f,
                0.0f, 1.0f, 1.0f, 2.0f, 2.0f,
                3.5f, 7.5f, 1.0f, 1.5f, 0.5f,
                7.0f, 8.0f, 8.0f, 1.5f, 0.0f,
                0.0f, 7.0f, 3.5f, 0.0f, 0.0f,
        };

        BufferedImage image = SourceImageFactory.createOneBandedFloatImage(5, 5, sourceData);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        GeneralFilterFunction.Median median = new GeneralFilterFunction.Median(3, 3, 1, 1, null);
        RenderedOp op = GeneralFilterDescriptor.create(image, median, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
        assertNotNull(op);
        assertEquals(5, op.getWidth());
        assertEquals(5, op.getHeight());
        assertEquals(DataBuffer.TYPE_FLOAT, op.getSampleModel().getDataType());

        DataBufferFloat destBuffer = ((DataBufferFloat) op.getData().getDataBuffer());
        float[] resultData = destBuffer.getData();
        for (int i = 0; i < resultData.length; i++) {
            assertEquals("i=" + i, expectedData[i], resultData[i], 1.0e-6);
        }
    }

}