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

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class NdviOpTest {

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



    public static void addBand(Product product, String bandName) {
        Band a = new Band(bandName, ProductData.TYPE_FLOAT32, 10, 10);
        product.addBand(a);
    }

    public static void addBand(Product product, String bandName, int wavelength) {
            Band a = new Band(bandName, ProductData.TYPE_FLOAT32, 10, 10);
            a.setSpectralWavelength(wavelength);
            product.addBand(a);
        }
    }
