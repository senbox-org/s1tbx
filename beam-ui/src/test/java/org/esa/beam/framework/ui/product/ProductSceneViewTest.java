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
package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.PropertyMap;

import java.awt.geom.AffineTransform;

import junit.framework.TestCase;

import com.bc.ceres.core.ProgressMonitor;

public class ProductSceneViewTest extends TestCase {

    private VirtualBand r;
    private VirtualBand g;
    private VirtualBand b;

    @Override
    protected void setUp() throws Exception {
        final Product product = new Product("x", "y", 2, 3);

        r = new VirtualBand("r", ProductData.TYPE_FLOAT32, 2, 3, "0");
        g = new VirtualBand("g", ProductData.TYPE_FLOAT32, 2, 3, "0");
        b = new VirtualBand("b", ProductData.TYPE_FLOAT32, 2, 3, "0");

        product.addBand(r);
        product.addBand(g);
        product.addBand(b);

        r.ensureRasterData();
        g.ensureRasterData();
        b.ensureRasterData();
    }

    @Override
    protected void tearDown() throws Exception {
        r = null;
        g = null;
        b = null;
}

    public void testIsRGB() {
        ProductSceneView view;

        view = new ProductSceneView(new ProductSceneImage(r, new PropertyMap(), ProgressMonitor.NULL));
        assertFalse(view.isRGB());

        view = new ProductSceneView(new ProductSceneImage("RGB", r, g, b, new PropertyMap(), ProgressMonitor.NULL));
        assertTrue(view.isRGB());
    }

    public void testDispose() {
        final ProductSceneView view = new ProductSceneView(new ProductSceneImage(r, new PropertyMap(), ProgressMonitor.NULL));
        view.dispose();
        assertNull(view.getSceneImage());
    }


    public void testAffineTransformReplacesManualCalculation() {
        double modelOffsetX = 37.8;
        double modelOffsetY = -54.1;
        double viewScale = 2.5;

        final AffineTransform transform = new AffineTransform();
        transform.scale(viewScale, viewScale);
        transform.translate(-modelOffsetX, -modelOffsetY);

        double modelX = 10.4;
        double modelY = 2.9;

        double viewX = (modelX - modelOffsetX) * viewScale;
        double viewY = (modelY - modelOffsetY) * viewScale;

        final double[] result = new double[2];
        transform.transform(new double[] {modelX,  modelY}, 0, result, 0, 1);
        assertEquals(viewX, result[0], 1e-10);
        assertEquals(viewY, result[1], 1e-10);
    }
}
