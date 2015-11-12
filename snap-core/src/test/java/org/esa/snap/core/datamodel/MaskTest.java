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

package org.esa.snap.core.datamodel;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

public class MaskTest {
    @Test
    public void testMask() {
        Mask.ImageType imageType = new BufferedImageType();
        Mask mask = new Mask("WATER", 256, 128, imageType);
        assertEquals("WATER", mask.getName());
        assertEquals(256, mask.getRasterWidth());
        assertEquals(128, mask.getRasterHeight());
        assertSame(imageType, mask.getImageType());
        PropertyContainer imageConfig = mask.getImageConfig();
        assertNotNull(imageConfig);
        assertEquals(Color.RED, mask.getImageConfig().getValue("color"));
        assertEquals(0.5, (double)mask.getImageConfig().getValue("transparency"), 1.0e-6);
        MultiLevelImage image = mask.getSourceImage();
        assertNotNull(image);
        assertSame(image, mask.getGeophysicalImage());
        assertSame(null, mask.getValidMaskImage());
    }

    @Test
    public void testAbstractMaskImageType() {
        Mask.ImageType type = new NullImageType();
        PropertyContainer imageConfig = type.createImageConfig();
        assertEquals(Color.RED, imageConfig.getValue("color"));
        assertEquals(0.5, (double) imageConfig.getValue("transparency"), 1.0e-6);
    }
    
    @Test
    public void testRenameBand() {
        Product product = new Product("t", "d", 1, 1);
        Band band = product.addBand("b", ProductData.TYPE_INT8);
        Mask mask = new Mask("m", 1, 1, Mask.BandMathsType.INSTANCE);
        Mask.BandMathsType.setExpression(mask, "b == 2");
        product.getMaskGroup().add(mask);
        
        String expression = Mask.BandMathsType.getExpression(mask);
        assertEquals("b == 2", expression);
        band.setName("c");
        expression = Mask.BandMathsType.getExpression(mask);
        assertEquals("c == 2", expression);
    }

    @Test
    public void testReassignBandMathsExpression() {
        Product product = new Product("P", "P-Type", 4, 1);
        VirtualBand band = new VirtualBand("B", ProductData.TYPE_FLOAT32, 4, 1, "X");
        Mask mask = Mask.BandMathsType.create("M", "M-Descr", 4, 1, "B > 2", Color.GREEN, 0.5);
        product.getBandGroup().add(band);
        product.getMaskGroup().add(mask);

        assertEquals(0, mask.getSourceImage().getData().getSample(0, 0, 0));
        assertEquals(0, mask.getSourceImage().getData().getSample(1, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(2, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(3, 0, 0));

        mask.getImageConfig().setValue("expression", "B > 3");

        assertEquals(0, mask.getSourceImage().getData().getSample(0, 0, 0));
        assertEquals(0, mask.getSourceImage().getData().getSample(1, 0, 0));
        assertEquals(0, mask.getSourceImage().getData().getSample(2, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(3, 0, 0));

        band.setExpression("X + 2");

        assertEquals(0, mask.getSourceImage().getData().getSample(0, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(1, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(2, 0, 0));
        assertEquals(255, mask.getSourceImage().getData().getSample(3, 0, 0));
    }

    private static class NullImageType extends Mask.ImageType {

        private NullImageType() {
            super("Null");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }
    }

    private static class BufferedImageType extends Mask.ImageType {

        private BufferedImageType() {
            super("Buffered");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            BufferedImage image = new BufferedImage(mask.getRasterWidth(), mask.getRasterHeight(), BufferedImage.TYPE_BYTE_GRAY);
            return new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, 3));
        }
    }
}
