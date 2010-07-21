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

package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Assert2D;
import com.bc.ceres.glevel.MultiLevelSource;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.media.jai.TiledImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class ImageLayerTest {

    @Test
    public void testThatLayerOperatesWithNullImage() {
        final ImageLayer layer = new ImageLayer(MultiLevelSource.NULL);

        assertNotNull(layer.getMultiLevelSource());
        assertNotNull(layer.getImage());

        assertNotNull(layer.getImageToModelTransform());
        assertTrue(layer.getImageToModelTransform().isIdentity());

        assertNotNull(layer.getModelToImageTransform());
        assertTrue(layer.getModelToImageTransform().isIdentity());

        assertNull(layer.getModelBounds());
    }

    @Test
    public void testConstructors() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertSame(image, layer.getImage());
        assertEquals(new AffineTransform(), layer.getModelToImageTransform());
        assertEquals(new AffineTransform(), layer.getImageToModelTransform());


        final AffineTransform i2m = AffineTransform.getTranslateInstance(+100, +200);
        layer = new ImageLayer(image, i2m, 1);
        assertSame(image, layer.getImage());
        assertNotSame(i2m, layer.getImageToModelTransform());
        assertNotSame(i2m, layer.getModelToImageTransform());
        assertEquals(AffineTransform.getTranslateInstance(+100, +200), layer.getImageToModelTransform());
        assertEquals(AffineTransform.getTranslateInstance(-100, -200), layer.getModelToImageTransform());
    }

    @Test
    public void testBoundingBox() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertNotNull(layer.getModelBounds());
        Assert2D.assertEquals(new Rectangle2D.Double(0.0, 0.0, 320.0, 200.0), layer.getModelBounds());

        final AffineTransform i2m = new AffineTransform(0.5, 0, 0, 0.5, -25.5, 50.3);
        layer = new ImageLayer(image, i2m, 1);
        assertNotNull(layer.getModelBounds());
        Assert2D.assertEquals(new Rectangle2D.Double(-25.5, 50.3, 160.0, 100.0), layer.getModelBounds());
    }
}
