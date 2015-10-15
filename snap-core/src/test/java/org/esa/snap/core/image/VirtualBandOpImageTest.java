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

package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

public class VirtualBandOpImageTest {

    private Band band;

    @Before
    public void before() {
        final Product product = new Product("P", "T", 1024, 1024);
        band = product.addBand("pixX", "((Y-0.5) * 1024) + (X-0.5)");
        product.setPreferredTileSize(512, 512);
    }

    @Test
    public void testComputationOnLevel0() {
        final RenderedImage image = band.getSourceImage().getImage(0);
        assertEquals(1024, image.getWidth());
        assertEquals(1024, image.getHeight());

        final Raster data = image.getData();
        final int sample = data.getSample(1023, 1023, 0);
        assertEquals(1048575, sample);    // 1023
    }

    @Test
    public void testComputationOnHigherLevel() {
        final RenderedImage image = band.getSourceImage().getImage(1);
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());

        final Raster data = image.getData();
        final int sample = data.getSample(511, 511, 0);
        assertEquals(1047550, sample);     // (511*2) * 1024 + (511*2)
    }


}
