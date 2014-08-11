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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.util.Arrays;

import static org.junit.Assert.*;

public class VirtualBandMultiLevelImageTest {

    private Product p;
    private Product q;
    private VirtualBand v;
    private VirtualBand w;
    private VirtualBandMultiLevelImage image;

    @Before
    public void setup() {
        p = new Product("P", "T", 1, 1);
        v = new VirtualBand("V", ProductData.TYPE_INT8, 1, 1, "1");
        p.addBand(v);

        q = new Product("Q", "T", 1, 1);
        w = new VirtualBand("W", ProductData.TYPE_INT8, 1, 1, "0");
        q.addBand(w);

        final ProductManager pm = new ProductManager();
        pm.addProduct(p);
        pm.addProduct(q);

        final String expression = "$1.V == $2.W";
        final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(v);
        image = new VirtualBandMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.createMask(expression, p, p.getSceneRasterWidth(), p.getSceneRasterHeight(),
                                                        ResolutionLevel.create(getModel(), level));
            }
        }, expression, p);
    }

    @Test
    public void imageIsUpdated() {
        assertTrue(0 == image.getImage(0).getData().getSample(0, 0, 0));
        w.setExpression("1");
        assertTrue(0 != image.getImage(0).getData().getSample(0, 0, 0));
    }

    @Test
    public void listenersAreAdded() {
        assertTrue(Arrays.asList(p.getProductNodeListeners()).contains(image));
        assertTrue(Arrays.asList(q.getProductNodeListeners()).contains(image));
    }

    @Test
    public void listenersAreRemoved() {
        image.dispose();
        assertFalse(Arrays.asList(p.getProductNodeListeners()).contains(image));
        assertFalse(Arrays.asList(q.getProductNodeListeners()).contains(image));
    }

    @Test
    public void nodesAreAdded() {
        assertTrue(image.getNodeMap().containsKey(p));
        assertTrue(image.getNodeMap().containsKey(q));
        assertTrue(image.getNodeMap().get(p).contains(v));
        assertTrue(image.getNodeMap().get(q).contains(w));
    }

    @Test
    public void nodesAreRemoved() {
        p.dispose();
        q.dispose();

        v = null;
        w = null;

        try {
            System.gc();
            Thread.sleep(100);
            assertTrue(image.getNodeMap().get(p).isEmpty());
            assertTrue(image.getNodeMap().get(q).isEmpty());
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void nodeMapIsCleared() {
        image.dispose();
        assertTrue(image.getNodeMap().isEmpty());
    }
}
