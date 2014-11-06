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

import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.RenderedImage;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;
import static org.junit.Assert.*;

public class ReinterpretDescriptorTest {

    private RenderedOp sourceImage;

    @Test
    public void testLinearRescaleUShort() {
        final RenderedImage targetImage = ReinterpretDescriptor.create(sourceImage, 17.0, 11.0, LINEAR, AWT, null);
        assertNotNull(targetImage);
        assertEquals(5, targetImage.getWidth());
        assertEquals(5, targetImage.getHeight());
        assertEquals(DataBuffer.TYPE_FLOAT, targetImage.getSampleModel().getDataType());

        final float[] targetData = ((DataBufferFloat) targetImage.getData().getDataBuffer()).getData();
        for (int i = 0; i < targetData.length; i++) {
            assertEquals("i = " + i, 130.0, targetData[i], 0.0);
        }
    }

    @Test
    public void testTargetImageRenderingIsSameAsSourceImageRendering() {
        final RenderedOp targetImage = ReinterpretDescriptor.create(sourceImage, 1.0, 0.0, LINEAR, AWT, null);
        assertSame(sourceImage.getRendering(), targetImage.getRendering());
    }

    @Before
    public void setup() {
        sourceImage = ConstantDescriptor.create(5f, 5f, new Short[]{7}, null);
    }
}
