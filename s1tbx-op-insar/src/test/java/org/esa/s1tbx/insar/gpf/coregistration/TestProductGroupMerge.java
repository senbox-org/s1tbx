/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestProductGroupMerge extends ProcessorTest  {

    @Test
    public void TestProductGroupMerge() throws Exception {

    }

    private Product createStack() throws IOException {
        final CreateStackOp op = (CreateStackOp) (new CreateStackOp.Spi()).createOperator();
        assertNotNull(op);

        int refW = 30, refH = 30;
        final Product refProduct = createTestProduct(refW, refH);
        final Product secProduct1 = createTestProduct(refW+10, refH+10);

        op.setSourceProducts(refProduct, secProduct1);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(refW, targetProduct.getSceneRasterWidth());
        assertEquals(refH, targetProduct.getSceneRasterHeight());

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        float[] pixels = new float[refW*refH];
        band.readPixels(0, 0, refW, refH, pixels, ProgressMonitor.NULL);

        assertEquals(1.5f, pixels[0], 0.0001f);
        assertEquals(11.5f, pixels[10], 0.0001f);
        assertEquals(101.5f, pixels[100], 0.0001f);

        return targetProduct;
    }

    private static Product createTestProduct(final int w, final int h) {

        Product product = TestUtils.createProduct("ASA_IMP_1P", w, h);
        TestUtils.createBand(product, "amplitude", ProductData.TYPE_FLOAT32, Unit.AMPLITUDE, w, h, true);
        return product;
    }
}
