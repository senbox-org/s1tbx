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

package com.bc.ceres.grender.support;

import com.bc.ceres.glayer.support.ImageLayer;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

import java.awt.Color;
import java.awt.image.BufferedImage;


@Ignore
public class BufferedImageRenderingTest  {
    public void testViewIsAnImage() {
        BufferedImage bi = new BufferedImage(2, 2, BufferedImage.TYPE_INT_BGR);
        bi.setRGB(0, 0, Color.ORANGE.getRGB());
        bi.setRGB(0, 1, Color.BLUE.getRGB());
        bi.setRGB(1, 0, Color.GREEN.getRGB());
        bi.setRGB(1, 1, Color.YELLOW.getRGB());

        final BufferedImageRendering rendering = new BufferedImageRendering(2, 2);
        ImageLayer il = new ImageLayer(bi);
        il.setTransparency(0.0);
        il.render(rendering);
        assertEquals(Color.ORANGE.getRGB(), rendering.getImage().getRGB(0, 0));
        assertEquals(Color.BLUE.getRGB(), rendering.getImage().getRGB(0, 1));
        assertEquals(Color.GREEN.getRGB(), rendering.getImage().getRGB(1, 0));
        assertEquals(Color.YELLOW.getRGB(), rendering.getImage().getRGB(1, 1));
    }
}
