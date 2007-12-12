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

    public void testIsRGB() throws IOException {
        ProductSceneView view;

        view = new ProductSceneView(ProductSceneImage.create(r, ProgressMonitor.NULL));
        assertFalse(view.isRGB());

        view = new ProductSceneView(ProductSceneImage.create(r, g, b, ProgressMonitor.NULL));
        assertTrue(view.isRGB());
    }

    public void testDispose() throws IOException {
        final ProductSceneView view;
        view = new ProductSceneView(ProductSceneImage.create(r, ProgressMonitor.NULL));

        view.dispose();
        assertNull(view.getScene());

        try {
            assertNull(view.getImageUpdateListeners());
            fail();
        } catch (NullPointerException expected) {
        }
    }
}
