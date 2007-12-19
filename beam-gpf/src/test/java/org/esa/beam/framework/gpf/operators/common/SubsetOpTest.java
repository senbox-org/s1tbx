/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.operators.common;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.Arrays;

public class SubsetOpTest extends TestCase {


    public void testConstructorUsage() throws Exception {
        final Product sp = createTestProduct(100, 100);

        final String[] bandNames = {"radiance_1", "radiance_3"};

        SubsetOp op = new SubsetOp();
        op.setSourceProduct(sp);
        op.setBandNames(bandNames);

        assertSame(sp, op.getSourceProduct());
        assertNotSame(bandNames, op.getBandNames());

        Product tp = op.getTargetProduct();

        assertEquals(2, tp.getNumBands());
        assertNotNull(tp.getBand("radiance_1"));
        assertNull(tp.getBand("radiance_2"));
        assertNotNull(tp.getBand("radiance_3"));
    }

    private Product createTestProduct(int w, int h) {
        Product testProduct = new Product("p", "t", w, h);

        Band band1 = testProduct.addBand("radiance_1", ProductData.TYPE_INT32);
        band1.setSynthetic(true);
        int[] intValues = new int[w * h];
        Arrays.fill(intValues, 1);
        band1.setData(ProductData.createInstance(intValues));

        Band band2 = testProduct.addBand("radiance_2", ProductData.TYPE_FLOAT32);
        band2.setSynthetic(true);
        float[] floatValues = new float[w * h];
        Arrays.fill(floatValues, 2.5f);
        band2.setData(ProductData.createInstance(floatValues));

        Band band3 = testProduct.addBand("radiance_3", ProductData.TYPE_INT16);
        band3.setScalingFactor(0.5);
        band3.setSynthetic(true);
        short[] shortValues = new short[w * h];
        Arrays.fill(shortValues, (short) 6);
        band3.setData(ProductData.createInstance(shortValues));

        return testProduct;
    }
}