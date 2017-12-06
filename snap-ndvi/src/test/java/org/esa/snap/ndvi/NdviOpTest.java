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

package org.esa.snap.ndvi;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class NdviOpTest {

    private static final float[] redValues = new float[] { 0, 1, 2, 3 };
    private static final float[] nirValues = new float[] { 1, 2, 3, 0 };

    @Test
    public void testFindBand() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 500);
        addBand(product, "b", 600);
        addBand(product, "c", 620);
        addBand(product, "d", 700);
        addBand(product, "e", 712);
        addBand(product, "f", 715);
        addBand(product, "g", 799);
        addBand(product, "h", 800);
        addBand(product, "i", 801);
        addBand(product, "j", 899);
        addBand(product, "k", 900);
        addBand(product, "l", 16000);

        assertEquals("b", NdviOp.findBand(600, 650, product));
        assertEquals("h", NdviOp.findBand(800, 900, product));
    }

    @Test
    public void testFindBand_nothingFound() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 500);
        addBand(product, "b", 600);
        addBand(product, "c", 620);
        addBand(product, "d", 700);
        addBand(product, "e", 712);
        addBand(product, "f", 715);
        addBand(product, "g", 799);
        addBand(product, "h", 800);
        addBand(product, "i", 801);
        addBand(product, "j", 899);
        addBand(product, "k", 900);

        assertNull(NdviOp.findBand(400, 499, product));
        assertNull(NdviOp.findBand(701, 711, product));
        assertNull(NdviOp.findBand(901, 12000, product));
    }

    @Test
    public void testFindBand_nothingFound_2() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a");
        addBand(product, "b");
        addBand(product, "c");
        addBand(product, "d");
        addBand(product, "e");
        addBand(product, "f");
        addBand(product, "g");
        addBand(product, "h");
        addBand(product, "i");
        addBand(product, "j");
        addBand(product, "k");
        addBand(product, "l");

        assertNull(NdviOp.findBand(600, 650, product));
    }

    @Test
    public void testTargetNoDataValue() {
        final Product sourceProduct = createTestProduct(4, 1, redValues, nirValues);

        final NdviOp op = new NdviOp();
        op.setSourceProduct(sourceProduct);
        op.setParameter("redFactor", 1.0f);
        op.setParameter("nirFactor", 1.0f);

        final Product targetProduct = op.getTargetProduct();
        final Band ndvi = targetProduct.getBand("ndvi");

        assertEquals(true, ndvi.isNoDataValueUsed());
        assertEquals(Float.NaN, ndvi.getSampleFloat(0, 0), 1e-6);
        assertEquals(0.3333333f, ndvi.getSampleFloat(1, 0), 1e-6);
        assertEquals(0.2f, ndvi.getSampleFloat(2, 0), 1e-6);
        assertEquals(Float.NaN, ndvi.getSampleFloat(3, 0), 1e-6);
    }

    private static void addBand(Product product, String bandName) {
        Band a = new Band(bandName, ProductData.TYPE_FLOAT32, 10, 10);
        product.addBand(a);
    }

    private static void addBand(Product product, String bandName, int wavelength) {
        Band a = new Band(bandName, ProductData.TYPE_FLOAT32, 10, 10);
        a.setSpectralWavelength(wavelength);
        product.addBand(a);
    }

    private static void addBand(Product product, String bandName, int wavelength, int width, int height, float[] values) {
        Band a = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
        a.setSpectralWavelength(wavelength);
        a.setRasterData(ProductData.createInstance(values));
        a.setNoDataValueUsed(true);
        a.setNoDataValue(0);
        product.addBand(a);
    }

    private static Product createTestProduct(int width, int height, float[] redValues, float[] nirValues) {
        final Product product = new Product("ProductName", "ProductType", width, height);
        addBand(product, "red", 600, width, height, redValues);
        addBand(product, "nir", 800, width, height, nirValues);

        return product;
    }
}
