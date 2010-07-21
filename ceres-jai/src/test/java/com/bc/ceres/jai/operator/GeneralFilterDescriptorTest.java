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

import junit.framework.TestCase;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBuffer;

import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import com.bc.ceres.jai.GeneralFilterFunction;


public class GeneralFilterDescriptorTest extends TestCase {

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
        RenderedOp op = GeneralFilterDescriptor.create(image, GeneralFilterFunction.MAX_3X3, new RenderingHints(JAI.KEY_BORDER_EXTENDER, borderExtender));
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

}