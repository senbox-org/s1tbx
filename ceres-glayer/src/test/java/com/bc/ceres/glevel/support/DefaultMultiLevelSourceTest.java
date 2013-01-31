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

package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


public class DefaultMultiLevelSourceTest {

    @Test
    public void testScaledImageSizes1() throws Exception {
        int W = 4096;
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(6, new AffineTransform(), new Rectangle2D.Double(0, 0, W, W));
        DefaultMultiLevelSource source = new DefaultMultiLevelSource(ConstantDescriptor.create((float) W, (float) W, new Byte[]{0}, null), model);

        // Sentinel-2 MSI 10m and 20m Tile
        testScaledImageSizes(4096, 0, source);
        testScaledImageSizes(2048, 1, source);
        testScaledImageSizes(1024, 2, source);
        testScaledImageSizes(512, 3, source);
        testScaledImageSizes(256, 4, source);
        testScaledImageSizes(128, 5, source);
    }

    @Test
    public void testScaledImageSizes2() throws Exception {
        int W = 1826;
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(6, new AffineTransform(), new Rectangle2D.Double(0, 0, W, W));
        DefaultMultiLevelSource source = new DefaultMultiLevelSource(ConstantDescriptor.create((float) W, (float) W, new Byte[]{0}, null), model);

        // Sentinel-2 MSI 60m Tile
        testScaledImageSizes(1826, 0, source);
        testScaledImageSizes(913, 1, source);
        testScaledImageSizes(457, 2, source);
        testScaledImageSizes(229, 3, source);
        testScaledImageSizes(115, 4, source);
        testScaledImageSizes(58, 5, source);
    }

    private void testScaledImageSizes(int expectedSize, int level, DefaultMultiLevelSource source) {
        Rectangle expectedRect = new Rectangle(0, 0, expectedSize, expectedSize);
        Rectangle sourceRect = new Rectangle(0, 0, source.getSourceImage().getWidth(), source.getSourceImage().getHeight());

        Rectangle j2kLevelRect = DefaultMultiLevelSource.getLevelImageBounds(sourceRect, source.getModel().getScale(level));
        assertEquals("at resolution level " + level + ":", expectedRect, j2kLevelRect);

        RenderedImage levelImage = source.getImage(level);
        Rectangle levelRect = new Rectangle(0, 0, levelImage.getWidth(), levelImage.getHeight());
        assertEquals("at resolution level " + level + ":", expectedRect, levelRect);
    }


    @Test
    public void testNull() {
        final MultiLevelSource mls = DefaultMultiLevelSource.NULL;
        assertEquals(1, mls.getModel().getLevelCount());
        assertNull(mls.getModel().getModelBounds());
    }

    @Test
    public void testLevelImages() {
        final PlanarImage src = createSourceImage(256, 128);

        DefaultMultiLevelSource mls = new DefaultMultiLevelSource(src, 5);
        assertEquals(5, mls.getModel().getLevelCount());

        assertSame(src, mls.getSourceImage());
        assertSame(src, mls.getImage(0));

        testLevelImage(mls, 0, 256, 128);
        testLevelImage(mls, 1, 128, 64);
        testLevelImage(mls, 2, 64, 32);
        testLevelImage(mls, 3, 32, 16);
        testLevelImage(mls, 4, 16, 8);
    }

    private void testLevelImage(DefaultMultiLevelSource mls, int level, int ew, int eh) {
        final RenderedImage image = mls.getImage(level);
        assertSame(image, mls.getImage(level));
        assertEquals(mls.getSourceImage().getSampleModel().getDataType(), image.getSampleModel().getDataType());
        assertEquals(mls.getSourceImage().getSampleModel().getNumBands(), image.getSampleModel().getNumBands());
        assertEquals(ew, image.getWidth());
        assertEquals(eh, image.getHeight());
    }

    static PlanarImage createSourceImage(int w, int h) {
        final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 0);
        bi.getRaster().setSample(1, 0, 0, 1);
        bi.getRaster().setSample(0, 1, 0, 2);
        bi.getRaster().setSample(1, 1, 0, 3);
        return PlanarImage.wrapRenderedImage(bi);
    }
}