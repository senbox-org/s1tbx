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

package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ProductUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProductFlipperTest {

    private static Product product;
    private static GeneralPath[] geoBoundaryPath;

    @BeforeClass
    public static void setup() {
        product = new Product("p", "t", 14, 16);
        TiePointGrid t1 = new TiePointGrid("t1", 3, 3, 0, 0, 5, 5, new float[]{
                0.6f, 0.3f, 0.4f,
                0.8f, 0.9f, 0.4f,
                0.3f, 0.2f, 0.4f
        });
        product.addTiePointGrid(t1);

        TiePointGrid t2 = new TiePointGrid("t2", 3, 3, 0, 0, 5, 5, new float[]{
                0.9f, 0.2f, 0.3f,
                0.6f, 0.1f, 0.4f,
                0.2f, 0.9f, 0.5f
        });
        product.addTiePointGrid(t2);
        product.setSceneGeoCoding(new TiePointGeoCoding(t1, t2));

        TiePointGrid t3 = new TiePointGrid("t3", 3, 3, 1, 1, 3, 3, new float[]{
                1, 2, 3,
                4, 5, 6,
                7, 8, 9
        });
        product.addTiePointGrid(t3);
        geoBoundaryPath = ProductUtils.createGeoBoundaryPaths(product);
    }

    @Test
    public void testTiePointFlipping_Horizontal() throws IOException {
        final Product flipped = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_HORIZONTAL, "h", "h");
        final TiePointGrid flippedT1 = flipped.getTiePointGrid("t1");
        final float[] expectedT1 = {
                0.4f, 0.3f, 0.6f,
                0.4f, 0.9f, 0.8f,
                0.4f, 0.2f, 0.3f
        };
        assertTrue(Arrays.equals(expectedT1, (float[]) flippedT1.getDataElems()));

        assertEquals(4, flippedT1.getOffsetX(), 1.0e-6);
        assertEquals(0, flippedT1.getOffsetY(), 1.0e-6);
        assertEquals(5, flippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(5, flippedT1.getSubSamplingY(), 1.0e-6);

        final TiePointGrid flippedT2 = flipped.getTiePointGrid("t3");
        assertEquals(7, flippedT2.getOffsetX(), 1.0e-6);
        assertEquals(1, flippedT2.getOffsetY(), 1.0e-6);
        assertEquals(3, flippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(3, flippedT2.getSubSamplingY(), 1.0e-6);

        GeneralPath[] flippedPath = ProductUtils.createGeoBoundaryPaths(flipped);
        assertEquals(1, flippedPath.length);
        final Rectangle2D expBounds = geoBoundaryPath[0].getBounds2D();
        final Rectangle2D actualBounds = flippedPath[0].getBounds2D();
        assertEquals(expBounds.getMinX(), actualBounds.getMinX(), 1.0e-6);
        assertEquals(expBounds.getMinY(), actualBounds.getMinY(), 1.0e-6);
        assertEquals(expBounds.getMaxX(), actualBounds.getMaxX(), 1.0e-6);
        assertEquals(expBounds.getMaxY(), actualBounds.getMaxY(), 1.0e-6);
    }

    @Test
    public void testTiePointFlipping_Vertical() throws IOException {
        final Product flipped = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_VERTICAL, "v", "v");
        final TiePointGrid flippedT1 = flipped.getTiePointGrid("t1");
        final float[] expectedT1 = {
                0.3f, 0.2f, 0.4f,
                0.8f, 0.9f, 0.4f,
                0.6f, 0.3f, 0.4f
        };
        assertTrue(Arrays.equals(expectedT1, (float[]) flippedT1.getDataElems()));

        assertEquals(0, flippedT1.getOffsetX(), 1.0e-6);
        assertEquals(6, flippedT1.getOffsetY(), 1.0e-6);
        assertEquals(5, flippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(5, flippedT1.getSubSamplingY(), 1.0e-6);

        final TiePointGrid flippedT2 = flipped.getTiePointGrid("t3");
        assertEquals(1, flippedT2.getOffsetX(), 1.0e-6);
        assertEquals(9, flippedT2.getOffsetY(), 1.0e-6);
        assertEquals(3, flippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(3, flippedT2.getSubSamplingY(), 1.0e-6);

        GeneralPath[] flippedPath = ProductUtils.createGeoBoundaryPaths(flipped);
        assertEquals(1, flippedPath.length);
        final Rectangle2D expBounds = geoBoundaryPath[0].getBounds2D();
        final Rectangle2D actualBounds = flippedPath[0].getBounds2D();
        assertEquals(expBounds.getMinX(), actualBounds.getMinX(), 1.0e-6);
        assertEquals(expBounds.getMinY(), actualBounds.getMinY(), 1.0e-6);
        assertEquals(expBounds.getMaxX(), actualBounds.getMaxX(), 1.0e-6);
        assertEquals(expBounds.getMaxY(), actualBounds.getMaxY(), 1.0e-6);
    }

    @Test
    public void testTiePointFlipping_Both() throws IOException {
        final Product flipped = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_BOTH, "b", "b");
        final TiePointGrid flippedT1 = flipped.getTiePointGrid("t1");
        final float[] expectedT1 = {
                0.4f, 0.2f, 0.3f,
                0.4f, 0.9f, 0.8f,
                0.4f, 0.3f, 0.6f
        };
        assertTrue(Arrays.equals(expectedT1, (float[]) flippedT1.getDataElems()));

        assertEquals(4, flippedT1.getOffsetX(), 1.0e-6);
        assertEquals(6, flippedT1.getOffsetY(), 1.0e-6);

        final TiePointGrid flippedT2 = flipped.getTiePointGrid("t3");
        assertEquals(7, flippedT2.getOffsetX(), 1.0e-6);
        assertEquals(9, flippedT2.getOffsetY(), 1.0e-6);

        GeneralPath[] flippedPath = ProductUtils.createGeoBoundaryPaths(flipped);
        assertEquals(1, flippedPath.length);
        final Rectangle2D expBounds = geoBoundaryPath[0].getBounds2D();
        final Rectangle2D actualBounds = flippedPath[0].getBounds2D();
        assertEquals(expBounds.getMinX(), actualBounds.getMinX(), 1.0e-6);
        assertEquals(expBounds.getMinY(), actualBounds.getMinY(), 1.0e-6);
        assertEquals(expBounds.getMaxX(), actualBounds.getMaxX(), 1.0e-6);
        assertEquals(expBounds.getMaxY(), actualBounds.getMaxY(), 1.0e-6);
    }

    @Test
    public void testTiePointFlipping_Sequence() throws IOException {
        final Product v = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_VERTICAL, "v", "v");
        final Product vh = ProductFlipper.createFlippedProduct(v, ProductFlipper.FLIP_HORIZONTAL, "vh", "vh");

        final TiePointGrid vhFlippedT1 = vh.getTiePointGrid("t1");
        assertEquals(4, vhFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(6, vhFlippedT1.getOffsetY(), 1.0e-6);

        final TiePointGrid vhFlippedT2 = vh.getTiePointGrid("t3");
        assertEquals(7, vhFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(9, vhFlippedT2.getOffsetY(), 1.0e-6);

        final Product vhb = ProductFlipper.createFlippedProduct(vh, ProductFlipper.FLIP_BOTH, "vhb", "vhb");

        final TiePointGrid vhbFlippedT1 = vhb.getTiePointGrid("t1");
        assertEquals(0, vhbFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(0, vhbFlippedT1.getOffsetY(), 1.0e-6);

        final TiePointGrid vhbFlippedT2 = vhb.getTiePointGrid("t3");
        assertEquals(1, vhbFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(1, vhbFlippedT2.getOffsetY(), 1.0e-6);

        GeneralPath[] flippedPath = ProductUtils.createGeoBoundaryPaths(vhb);
        assertEquals(1, flippedPath.length);
        assertEquals(geoBoundaryPath[0].getBounds2D(), flippedPath[0].getBounds2D());
    }

    @Test
    public void testTiePointFlipping_WithSubset() throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(2, 2, 10, 10);
        subsetDef.setSubSampling(2, 2);
        final Product sub = ProductSubsetBuilder.createProductSubset(product, subsetDef, "s", "s");
        final TiePointGrid sFlippedT1 = sub.getTiePointGrid("t1");
        assertEquals(-0.75, sFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(-0.75, sFlippedT1.getOffsetY(), 1.0e-6);
        assertEquals(2.5, sFlippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(2.5, sFlippedT1.getSubSamplingY(), 1.0e-6);

        final TiePointGrid sFlippedT2 = sub.getTiePointGrid("t3");
        assertEquals(-0.25, sFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(-0.25, sFlippedT2.getOffsetY(), 1.0e-6);
        assertEquals(1.5, sFlippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(1.5, sFlippedT2.getSubSamplingY(), 1.0e-6);


        final Product sv = ProductFlipper.createFlippedProduct(sub, ProductFlipper.FLIP_VERTICAL, "sv", "v");

        final TiePointGrid svFlippedT1 = sv.getTiePointGrid("t1");
        assertEquals(-0.75, svFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(0.75, svFlippedT1.getOffsetY(), 1.0e-6);
        assertEquals(2.5, svFlippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(2.5, svFlippedT1.getSubSamplingY(), 1.0e-6);

        final TiePointGrid svFlippedT2 = sv.getTiePointGrid("t3");
        assertEquals(-0.25, svFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(2.25, svFlippedT2.getOffsetY(), 1.0e-6);
        assertEquals(1.5, svFlippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(1.5, svFlippedT2.getSubSamplingY(), 1.0e-6);


        final Product sh = ProductFlipper.createFlippedProduct(sub, ProductFlipper.FLIP_HORIZONTAL, "sh", "vh");
        final TiePointGrid shFlippedT1 = sh.getTiePointGrid("t1");
        assertEquals(0.75, shFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(-0.75, shFlippedT1.getOffsetY(), 1.0e-6);
        assertEquals(2.5, shFlippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(2.5, shFlippedT1.getSubSamplingY(), 1.0e-6);

        final TiePointGrid shFlippedT2 = sh.getTiePointGrid("t3");
        assertEquals(2.25, shFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(-0.25, shFlippedT2.getOffsetY(), 1.0e-6);
        assertEquals(1.5, shFlippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(1.5, shFlippedT2.getSubSamplingY(), 1.0e-6);

        final Product sb = ProductFlipper.createFlippedProduct(sub, ProductFlipper.FLIP_BOTH, "sb", "sb");

        final TiePointGrid sbFlippedT1 = sb.getTiePointGrid("t1");
        assertEquals(0.75, sbFlippedT1.getOffsetX(), 1.0e-6);
        assertEquals(0.75, sbFlippedT1.getOffsetY(), 1.0e-6);
        assertEquals(2.5, sbFlippedT1.getSubSamplingX(), 1.0e-6);
        assertEquals(2.5, sbFlippedT1.getSubSamplingY(), 1.0e-6);


        final TiePointGrid sbFlippedT2 = sb.getTiePointGrid("t3");
        assertEquals(2.25, sbFlippedT2.getOffsetX(), 1.0e-6);
        assertEquals(2.25, sbFlippedT2.getOffsetY(), 1.0e-6);
        assertEquals(1.5, sbFlippedT2.getSubSamplingX(), 1.0e-6);
        assertEquals(1.5, sbFlippedT2.getSubSamplingY(), 1.0e-6);
    }

    @Test
    public void testCreateFlipped_wrongFlipType() throws IOException {
        try {
            ProductFlipper.createFlippedProduct(product, 0, "bla", "blub");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
