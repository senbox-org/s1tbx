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

    public void testIsRGB() throws IOException {
        ProductSceneView productSceneView;

        final Product product = new Product("x", "y", 2, 3);
        final VirtualBand redRaster = new VirtualBand("bn1", ProductData.TYPE_FLOAT32, 2, 3, "0");
        final VirtualBand greenRaster = new VirtualBand("bn2", ProductData.TYPE_FLOAT32, 2, 3, "0");
        final VirtualBand blueRaster = new VirtualBand("bn3", ProductData.TYPE_FLOAT32, 2, 3, "0");
        product.addBand(redRaster);
        product.addBand(greenRaster);
        product.addBand(blueRaster);
        redRaster.ensureRasterData();
        greenRaster.ensureRasterData();
        blueRaster.ensureRasterData();

        ProductSceneImage sceneImage;

        sceneImage = ProductSceneImage.create(redRaster, ProgressMonitor.NULL);
        productSceneView = new ProductSceneView(sceneImage);
        assertFalse(productSceneView.isRGB());

        sceneImage = ProductSceneImage.create(redRaster, greenRaster, blueRaster, ProgressMonitor.NULL);
        productSceneView = new ProductSceneView(sceneImage);
        assertTrue(productSceneView.isRGB());
    }
}
