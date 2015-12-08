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

package org.esa.snap.core.datamodel;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Term;
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
    public void setup() throws ParseException {
        p = new Product("P", "T", 1, 1);
        v = new VirtualBand("V", ProductData.TYPE_INT8, 1, 1, "1");
        p.addBand(v);

        q = new Product("Q", "T", 1, 1);
        w = new VirtualBand("W", ProductData.TYPE_INT8, 1, 1, "0");
        q.addBand(w);

        ProductManager pm = new ProductManager();
        pm.addProduct(p);
        pm.addProduct(q);

        String expression = "$1.V == $2.W";

        Term term = VirtualBandOpImage.parseExpression(expression, p);

        MultiLevelModel multiLevelModel = v.getMultiLevelModel();
        image = new VirtualBandMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.builder(term)
                        .mask(true)
                        .level(ResolutionLevel.create(getModel(), level))
                        .sourceSize(p.getSceneRasterSize())
                        .create();
            }
        }, term);
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
        assertTrue(image.getReferencedProducts().contains(p));
        assertTrue(image.getReferencedProducts().contains(q));
        assertTrue(image.getReferencedRasters().contains(v));
        assertTrue(image.getReferencedRasters().contains(w));
    }

    @Test
    public void nodeMapIsCleared() {
        image.dispose();
        assertTrue(image.getReferencedProducts().isEmpty());
        assertTrue(image.getReferencedRasters().isEmpty());
    }

    @Test
    public void productRefsAreRemoved() {

        p.dispose();
        q.dispose();

        p = null;
        q = null;

        v = null;
        w = null;

        try {
            System.gc();
            Thread.sleep(100);
            assertTrue(image.getReferencedProducts().isEmpty());
        } catch (Exception e) {
            // ignore
        }
    }
}
