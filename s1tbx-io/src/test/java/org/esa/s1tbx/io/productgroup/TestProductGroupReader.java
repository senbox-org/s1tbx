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
package org.esa.s1tbx.io.productgroup;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


public class TestProductGroupReader extends ProcessorTest {

    @Test
    public void testRead() throws Exception {
        final File file = new File("C:\\out\\productgroups\\product_group.json");

        Product product = ProductIO.readProduct(file);
        assertNotNull(product);

        assertEquals(4, product.getNumBands());
        assertEquals("band1", product.getBandAt(0).getName());
        assertEquals("band2", product.getBandAt(1).getName());
        assertEquals("band3", product.getBandAt(2).getName());
        assertEquals("band4", product.getBandAt(3).getName());
    }

}
