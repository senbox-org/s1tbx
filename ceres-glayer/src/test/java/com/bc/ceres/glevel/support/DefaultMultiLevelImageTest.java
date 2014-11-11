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
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class DefaultMultiLevelImageTest extends TestCase {

    public void testSamplesAreProvidedFromSource() {
        PlanarImage sourceImage = createSingleBandedByteImage(2, 2);
        DefaultMultiLevelSource mls = new DefaultMultiLevelSource(sourceImage, 1);
        DefaultMultiLevelImage mli = new DefaultMultiLevelImage(mls);

        assertSame(mls, mli.getSource());
        assertSame(sourceImage, mli.getImage(0));
        assertSame(mls.getImage(0), mli.getImage(0));

        assertEquals(11, mli.getData().getSample(0, 0, 0));
        assertEquals(22, mli.getData().getSample(1, 0, 0));
        assertEquals(33, mli.getData().getSample(0, 1, 0));
        assertEquals(44, mli.getData().getSample(1, 1, 0));
    }

    public void testLevelInstances() {
        DefaultMultiLevelImage mli = createSomeDefaultMultiLevelImage();

        RenderedImage a0 = mli.getImage(0);
        assertNotNull(a0);
        assertSame(a0, mli.getImage(0));
        RenderedImage a1 = mli.getImage(1);
        assertNotNull(a1);
        assertSame(a1, mli.getImage(1));

        mli.reset();

        RenderedImage b0 = mli.getImage(0);
        assertNotNull(b0);
        assertSame(b0, mli.getImage(0));
        assertNotSame(a0, b0);

        RenderedImage b1 = mli.getImage(1);
        assertNotNull(b1);
        assertSame(b1, mli.getImage(1));
        assertNotSame(a1, b1);
    }

    public void testProperties() {
        DefaultMultiLevelImage mli = createSomeDefaultMultiLevelImage();
        PCL pcl = new PCL();

        assertEquals(Image.UndefinedProperty, mli.getProperty("_x"));

        mli.addPropertyChangeListener(pcl);
        mli.setProperty("_x", 4384);

        assertEquals(4384, mli.getProperty("_x"));
        assertEquals(Integer.class, mli.getPropertyClass("_x"));
        assertEquals("_x;", pcl.trace);

        String[] propertyNames = mli.getPropertyNames();
        assertEquals(1, propertyNames.length);
        assertEquals("_x", propertyNames[0]);

        propertyNames = mli.getPropertyNames("_");
        assertEquals(1, propertyNames.length);
        assertEquals("_x", propertyNames[0]);

        mli.removeProperty("_x");
        assertEquals(Image.UndefinedProperty, mli.getProperty("_x"));
    }

    private DefaultMultiLevelImage createSomeDefaultMultiLevelImage() {
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(2, new AffineTransform(), 256, 256);
        MultiLevelSource mls = new TestMultiLevelSource(model);
        return new DefaultMultiLevelImage(mls);
    }

    static PlanarImage createSingleBandedByteImage(int w, int h) {
        final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 11);
        bi.getRaster().setSample(1, 0, 0, 22);
        bi.getRaster().setSample(0, 1, 0, 33);
        bi.getRaster().setSample(1, 1, 0, 44);
        return PlanarImage.wrapRenderedImage(bi);
    }

    private static class TestMultiLevelSource extends AbstractMultiLevelSource {
        public TestMultiLevelSource(DefaultMultiLevelModel model) {
            super(model);
        }

        @Override
        protected RenderedImage createImage(int level) {
            // todo - The knowledge how width and height are computed should go either into AbstractMultiLevelSource or into the MultiLevelModel (nf 20090113)
            int width = (int) Math.floor(256 / getModel().getScale(level));
            int height = (int) Math.floor(256 / getModel().getScale(level));
            return createSingleBandedByteImage(width, height);
        }
    }

    private static class PCL implements PropertyChangeListener {
        String trace = "";

        public void propertyChange(PropertyChangeEvent evt) {
            trace += evt.getPropertyName() + ";";
        }
    }
}
