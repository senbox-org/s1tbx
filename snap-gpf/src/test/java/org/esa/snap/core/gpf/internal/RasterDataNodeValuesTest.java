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
package org.esa.snap.core.gpf.internal;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

public class RasterDataNodeValuesTest extends TestCase {

    public void testgetNames() {
        Product testProduct = new Product("name", "desc", 1, 1);
        testProduct.addBand("a", ProductData.TYPE_INT8);
        testProduct.addBand("b", ProductData.TYPE_INT8);
        
        String[] bandNames = RasterDataNodeValues.getNames(testProduct, Band.class);
        
        assertNotNull(bandNames);
        assertEquals(2, bandNames.length);
        assertEquals("a", bandNames[0]);
        assertEquals("b", bandNames[1]);
        
        bandNames = RasterDataNodeValues.getNames(testProduct, Band.class, true);
        
        assertNotNull(bandNames);
        assertEquals(3, bandNames.length);
        assertEquals("", bandNames[0]);
        assertEquals("a", bandNames[1]);
        assertEquals("b", bandNames[2]);
        
        String[] maskNames = RasterDataNodeValues.getNames(testProduct, Mask.class);
        assertNotNull(maskNames);
        assertEquals(0, maskNames.length);
    }
}
