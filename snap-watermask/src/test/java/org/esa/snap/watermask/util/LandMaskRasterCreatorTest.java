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

package org.esa.snap.watermask.util;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
@SuppressWarnings({"ReuseOfLocalVariable"})
public class LandMaskRasterCreatorTest {

    @Test
    public void testMixedFile() throws Exception {
        final BufferedImage image = ImageIO.read(getClass().getResource("156-12.png"));
        int sample = image.getData().getSample(220, 40, 0);
        assertEquals(0, sample);

        sample = image.getData().getSample(220, 41, 0);
        assertEquals(1, sample);

        sample = image.getData().getSample(187, 89, 0);
        assertEquals(0, sample);

        sample = image.getData().getSample(186, 89, 0);
        assertEquals(1, sample);
        sample = image.getData().getSample(188, 89, 0);
        assertEquals(1, sample);
    }

    @Test
    public void testAllWaterFile() throws Exception {
        final BufferedImage image = ImageIO.read(getClass().getResource("195-10.png"));
        Raster imageData = image.getData();
        for (int x = 0; x < imageData.getWidth(); x++) {
            for (int y = 0; y < imageData.getHeight(); y++) {
                assertEquals(0, imageData.getSample(x, y, 0));
            }
        }
    }

    @Test
    public void testAllLandFile() throws Exception {
        final BufferedImage image = ImageIO.read(getClass().getResource("92-10.png"));
        Raster imageData = image.getData();
        for (int x = 0; x < imageData.getWidth(); x++) {
            for (int y = 0; y < imageData.getHeight(); y++) {
                assertEquals(1, imageData.getSample(x, y, 0));
            }
        }
    }
}
