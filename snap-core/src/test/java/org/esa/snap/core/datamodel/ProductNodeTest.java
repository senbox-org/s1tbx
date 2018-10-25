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

package org.esa.snap.core.datamodel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.awt.Color;

public class ProductNodeTest extends TestCase {

    public ProductNodeTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductNodeTest.class);
    }

    @Override
    protected void setUp() {
    }

    public void testSetOwnerToNullAfterNodeRemoval() {
        final ProductNode[] owners = new ProductNode[1];
        final Product product = new Product("product", "t", 10, 10);
        product.addBand("band1", ProductData.TYPE_INT16);
        final Band addedBand = product.getBandAt(0);

        product.addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                ProductNode sourceNode = event.getSourceNode();
                assertSame(addedBand, sourceNode);
                assertNotNull(sourceNode.getOwner());
            }
        });

        assertNotNull(addedBand.getOwner());
        assertSame(product, addedBand.getOwner().getProduct());
        assertSame(product, addedBand.getProduct());

        product.removeBand(addedBand);
        assertNull(addedBand.getProduct());
        assertNull(addedBand.getOwner());
    }

    public void testSetProductToNullAfterNodeRemoval() {

        Band band = new Band("b", ProductData.TYPE_INT16, 10, 10);

        assertNull(band.getOwner());
        assertNull(band.getProduct());

        final Product p1 = new Product("p1", "t", 10, 10);
        p1.addBand(band);

        assertNotNull(band.getOwner());
        assertSame(p1, band.getProduct());

        p1.removeBand(band);

        assertNull(band.getOwner());
        assertNull(band.getProduct());

        final Product p2 = new Product("p2", "t", 10, 10);
        p2.addBand(band);

        assertNotNull(band.getOwner());
        assertSame(p2, band.getProduct());

        p2.removeBand(band);

        assertNull(band.getOwner());
        assertNull(band.getProduct());
    }

    public void testSetName() {
        int numberOfExceptionsTrown;
        int expectedNumberOfExceptions;
        final ProductNode productNode = new Band("valid", ProductData.TYPE_INT8, 1, 1);


        final String[] invalidNames = new String[]{
            ".Band", "",  " ",  "       ", "or", "not", "and"
        };
        numberOfExceptionsTrown = tryToSetInvalidNames(productNode, invalidNames);
        expectedNumberOfExceptions = invalidNames.length;
        assertEquals(expectedNumberOfExceptions, numberOfExceptionsTrown);


        final String[] validNames = new String[]{
            "Band", "band1", "ba_nd", "band_", "_band", "Band.sdf",
            "1band", "ba#nd", "band~", "band 2", "band ", " band"
        };
        numberOfExceptionsTrown = tryToSetValidNames(productNode, validNames);
        expectedNumberOfExceptions = 0;
        assertEquals(expectedNumberOfExceptions, numberOfExceptionsTrown);
    }

    public void testSetName_NodeInProduct() {
        Product product = new Product("P", "T", 10, 10);
        product.addBand("band", ProductData.TYPE_INT16);
        product.addTiePointGrid(new TiePointGrid("tpg", 5,5, 0,0,2,2));
        product.addMask("mask", "True", "test", Color.CYAN, 0.6);

        Band productNode = new Band("valid", ProductData.TYPE_INT8, 10, 10);
        product.addBand(productNode);

        try {
            productNode.setName("band");
            fail("Band with name 'band' already exists!");
        } catch (IllegalArgumentException ignore) {}

        try {
            productNode.setName("tpg");
            fail("Tie-point grid with name 'tpg' already exists!");
        } catch (IllegalArgumentException ignore) {}

        try {
            productNode.setName("mask");
            fail("mask with name 'mask' already exists!");
        } catch (IllegalArgumentException ignore) {}

        productNode.setName("valid");
    }

    public void testIsValidNodeName(){
        assertFalse(ProductNode.isValidNodeName(""));
        assertFalse(ProductNode.isValidNodeName(" "));
        assertFalse(ProductNode.isValidNodeName("\\"));
        assertFalse(ProductNode.isValidNodeName("/"));
        assertFalse(ProductNode.isValidNodeName("*"));
        assertFalse(ProductNode.isValidNodeName("?"));
        assertFalse(ProductNode.isValidNodeName("\""));
        assertFalse(ProductNode.isValidNodeName(":"));
        assertFalse(ProductNode.isValidNodeName("<"));
        assertFalse(ProductNode.isValidNodeName(">"));
        assertFalse(ProductNode.isValidNodeName("|"));
        assertFalse(ProductNode.isValidNodeName("."));
        assertFalse(ProductNode.isValidNodeName(".a"));

        assertTrue(ProductNode.isValidNodeName("a"));
        assertTrue(ProductNode.isValidNodeName("a."));
        assertTrue(ProductNode.isValidNodeName("1"));
        assertTrue(ProductNode.isValidNodeName("F"));
        assertTrue(ProductNode.isValidNodeName("_"));
        assertTrue(ProductNode.isValidNodeName("-"));
        assertTrue(ProductNode.isValidNodeName("$"));
        assertTrue(ProductNode.isValidNodeName("\u20ac")); // Euro
        assertTrue(ProductNode.isValidNodeName("@"));
        assertTrue(ProductNode.isValidNodeName("+"));
        assertTrue(ProductNode.isValidNodeName("~"));
    }

    private int tryToSetInvalidNames(final ProductNode productNode, final String[] names) {
        int countedExceptions = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                productNode.setName(names[i]);
                fail("IllegalArgumentException expected for name '" + names[i] + "'");
            } catch (IllegalArgumentException e) {
                countedExceptions++;
            }
        }
        return countedExceptions;
    }

    private int tryToSetValidNames(final ProductNode productNode, final String[] names) {
        int countedExceptions = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                productNode.setName(names[i]);
            } catch (IllegalArgumentException e) {
                countedExceptions++;
                fail("IllegalArgumentException was NOT expected for name '" + names[i] + "'");
            }
        }
        return countedExceptions;
    }
}

