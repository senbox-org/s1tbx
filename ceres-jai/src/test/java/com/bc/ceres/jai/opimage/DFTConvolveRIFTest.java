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

package com.bc.ceres.jai.opimage;

import junit.framework.TestCase;

import javax.media.jai.KernelJAI;
import javax.media.jai.TiledImage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

public class DFTConvolveRIFTest extends TestCase {
    public void testKernelImage() {
        float sum = 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9;

        float[] data = {
                1 / sum, 2 / sum, 3 / sum,
                4 / sum, 5 / sum, 6 / sum,
                7 / sum, 8 / sum, 9 / sum,
        };

        TiledImage image = DFTConvolveRIF.createKernelImage(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY),
                                                            DataBuffer.TYPE_FLOAT,
                                                            new KernelJAI(3, 3, data));
        assertEquals(10, image.getWidth());
        assertEquals(10, image.getHeight());

        // Upper left 3x3 pixels
        assertEquals(5 / sum, image.getSampleFloat(0, 0, 0), 0.0f);
        assertEquals(6 / sum, image.getSampleFloat(1, 0, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(2, 0, 0), 0.0f);
        assertEquals(8 / sum, image.getSampleFloat(0, 1, 0), 0.0f);
        assertEquals(9 / sum, image.getSampleFloat(1, 1, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(2, 1, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(0, 2, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(1, 2, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(2, 2, 0), 0.0f);

        // Upper right 2x3 pixels
        assertEquals(0.0f, image.getSampleFloat(8, 0, 0), 0.0f);
        assertEquals(4 / sum, image.getSampleFloat(9, 0, 0), 0.0f);
        assertEquals(0.0f , image.getSampleFloat(8, 1, 0), 0.0f);
        assertEquals(7 / sum, image.getSampleFloat(9, 1, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(8, 2, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(9, 2, 0), 0.0f);

        // Lower left 3x2 pixels
        assertEquals(0.0f, image.getSampleFloat(0, 8, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(1, 8, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(2, 8, 0), 0.0f);
        assertEquals(2 / sum, image.getSampleFloat(0, 9, 0), 0.0f);
        assertEquals(3 / sum, image.getSampleFloat(1, 9, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(2, 9, 0), 0.0f);

        // Lower right 2x2 pixels
        assertEquals(0.0f, image.getSampleFloat(8, 8, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(9, 8, 0), 0.0f);
        assertEquals(0.0f, image.getSampleFloat(8, 9, 0), 0.0f);
        assertEquals(1 / sum, image.getSampleFloat(9, 9, 0), 0.0f);

    }

    public void testNextBase2Size() {
        assertEquals(256, DFTConvolveRIF.getNextBase2Size(256));
        assertEquals(512, DFTConvolveRIF.getNextBase2Size(257));
        assertEquals(512, DFTConvolveRIF.getNextBase2Size(512));
        assertEquals(1024, DFTConvolveRIF.getNextBase2Size(513));
        assertEquals(1024, DFTConvolveRIF.getNextBase2Size(512 + 2 * 33));
    }

}
