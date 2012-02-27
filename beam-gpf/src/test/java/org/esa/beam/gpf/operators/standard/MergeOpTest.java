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

package org.esa.beam.gpf.operators.standard;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MergeOpTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGraph() throws Exception {
        final String xml = "<graph id=\"someGraphId\">\n" +
                         "    <version>1.0</version>\n" +
                         "    <node id=\"someNodeId\">\n" +
                         "      <operator>Merge</operator>\n" +
                         "      <sources/>\n" +
                         "      <parameters>\n" +
                         "        <includes>\n" +
                         "          <include>\n" +
                         "            <pattern>radiance_(.*)</pattern>\n" +
                         "            <renameTo>rad_$1</renameTo>\n" +
                         "          </include>\n" +
                         "        </includes>\n" +
                         "      </parameters>\n" +
                         "    </node>\n" +
                         "  </graph>";
    }

    @Test
    public void testMergeOp() throws Exception {
        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 10, 10);
        
        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProduct("dummy1", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("B"));
        assertEquals("dummy1", mergedProduct.getName());
        assertSame(productA, mergedProduct);
    }

    @Test(expected = OperatorException.class)
    public void testValidateSourceProducts_Failing() throws Exception {
        final MergeOp mergeOp = new MergeOp();

        final Product productA = new Product("dummy1", "mergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "mergeOpTest", 11, 11);

        mergeOp.setSourceProducts(productA, productB);
        mergeOp.getTargetProduct();
    }
}
