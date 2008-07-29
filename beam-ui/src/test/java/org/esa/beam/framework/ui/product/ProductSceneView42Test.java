/*
 * $Id: ProductSceneViewTest.java,v 1.3 2006/12/15 08:41:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;

import java.io.IOException;
import java.awt.geom.AffineTransform;

public class ProductSceneView42Test extends TestCase {

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

    public void testIsRGB() throws IOException {
        ProductSceneView view;

        view = ProductSceneView.create(ProductSceneImage42.create(r, ProgressMonitor.NULL, ProductSceneImage.isInTiledImagingMode()));
        assertFalse(view.isRGB());

        view = ProductSceneView.create(ProductSceneImage42.create(r, g, b, ProgressMonitor.NULL));
        assertTrue(view.isRGB());
    }

    public void testDispose() throws IOException {
        final ProductSceneView view;
        view = ProductSceneView.create(ProductSceneImage42.create(r, ProgressMonitor.NULL, ProductSceneImage.isInTiledImagingMode()));

        view.dispose();
        assertNull(view.getSceneImage());

        try {
            assertNull(view.getImageUpdateListeners());
            fail();
        } catch (NullPointerException expected) {
        }
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
