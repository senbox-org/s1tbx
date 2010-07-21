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
import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;


public class DefaultMultiLevelSourceTest extends TestCase {

    public void testNull() {
        final MultiLevelSource mls = DefaultMultiLevelSource.NULL;
        assertEquals(1, mls.getModel().getLevelCount());
        assertNull(mls.getModel().getModelBounds());
    }

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