/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import junit.framework.TestCase;

public class ProductNodeValuesTest extends TestCase {

    public void testgetNames() {
        Product testProduct = new Product("name", "desc", 1, 1);
        testProduct.addBand("a", ProductData.TYPE_INT8);
        testProduct.addBand("b", ProductData.TYPE_INT8);
        
        String[] bandNames = ProductNodeValues.getNames(testProduct, Band.class);
        
        assertNotNull(bandNames);
        assertEquals(2, bandNames.length);
        assertEquals("a", bandNames[0]);
        assertEquals("b", bandNames[1]);
        
        bandNames = ProductNodeValues.getNames(testProduct, Band.class, true);
        
        assertNotNull(bandNames);
        assertEquals(3, bandNames.length);
        assertEquals("", bandNames[0]);
        assertEquals("a", bandNames[1]);
        assertEquals("b", bandNames[2]);
        
        String[] maskNames = ProductNodeValues.getNames(testProduct, Mask.class);
        assertNotNull(maskNames);
        assertEquals(0, maskNames.length);
    }
}
