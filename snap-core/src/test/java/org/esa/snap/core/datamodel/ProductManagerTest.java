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
import java.util.Vector;

public class ProductManagerTest extends TestCase {

    private static final String _prodName = "TestProduct";
    private static final int _sceneWidth = 400;
    private static final int _sceneHeight = 300;

    private ProductManager productManager;
    private Product product1;
    private Product product2;
    private Product product3;

    public ProductManagerTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductManagerTest.class);
    }

    /**
     * Initialization for the tests.
     */
    @Override
    protected void setUp() {
        productManager = new ProductManager();
        product1 = new Product("product1", _prodName, _sceneWidth, _sceneHeight);
        product2 = new Product("product2", _prodName, _sceneWidth, _sceneHeight);
        product3 = new Product("product3", _prodName, _sceneWidth, _sceneHeight);
    }

    @Override
    protected void tearDown() {

    }

    public void testAddProduct() {
        final ProductManagerListener listener = new ProductManagerListener();
        productManager.addListener(listener);

        productManager.addProduct(product1);

        assertEquals(1, productManager.getProductCount());
        assertSame(product1, productManager.getProduct(0));
        assertSame(product1, productManager.getProduct("product1"));
        assertEquals(1, product1.getRefNo());
        assertSame(productManager, product1.getProductManager());

        final Vector<Product> addedProducts = listener.getAddedProducts();
        assertEquals(1, addedProducts.size());
        assertSame(product1, addedProducts.get(0));

        final Vector<Product> removedProducts = listener.getRemovedProducts();
        assertEquals(0, removedProducts.size());
    }

    public void testRemoveProduct() {
        addAllProducts();

        final ProductManagerListener listener = new ProductManagerListener();
        productManager.addListener(listener);

        productManager.removeProduct(product2);

        assertEquals(2, productManager.getProductCount());
        assertSame(product1, productManager.getProduct(0));
        assertSame(product3, productManager.getProduct(1));
        assertSame(product1, productManager.getProduct("product1"));
        assertNull(productManager.getProduct("product2"));
        assertSame(product3, productManager.getProduct("product3"));
        assertEquals(1, product1.getRefNo());
        assertEquals(0, product2.getRefNo());
        assertEquals(3, product3.getRefNo());
        assertSame(productManager, product1.getProductManager());
        assertNull(product2.getProductManager());
        assertSame(productManager, product3.getProductManager());

        final Vector<Product> addedProducts = listener.getAddedProducts();
        assertEquals(0, addedProducts.size());

        final Vector<Product> removedProducts = listener.getRemovedProducts();
        assertEquals(1, removedProducts.size());
        assertSame(product2, removedProducts.get(0));
    }

    public void testRemoveAll() {
        addAllProducts();
        final ProductManagerListener listener = new ProductManagerListener();
        productManager.addListener(listener);
        productManager.removeAllProducts();

        assertEquals(0, productManager.getProductCount());

        assertNull(product1.getProductManager());
        assertNull(product2.getProductManager());
        assertNull(product3.getProductManager());

        assertEquals(0, product1.getRefNo());
        assertEquals(0, product2.getRefNo());
        assertEquals(0, product3.getRefNo());


        final Vector<Product> removedProducts = listener.getRemovedProducts();
        assertEquals(3, removedProducts.size());
        assertSame(product1, removedProducts.get(0));
        assertSame(product2, removedProducts.get(1));
        assertSame(product3, removedProducts.get(2));

        final Vector<Product> addedProducts = listener.getAddedProducts();
        assertEquals(0, addedProducts.size());
    }

    public void testContainsProduct() {
        assertEquals(false, productManager.containsProduct("product2"));

        productManager.addProduct(product2);
        assertEquals(true, productManager.containsProduct("product2"));

        productManager.removeProduct(product2);
        assertEquals(false, productManager.containsProduct("product2"));
    }

    public void testGetNumProducts() {
        assertEquals(0, productManager.getProductCount());
        addAllProducts();
        assertEquals(3, productManager.getProductCount());
        productManager.removeProduct(product1);
        assertEquals(2, productManager.getProductCount());
        productManager.removeProduct(product2);
        assertEquals(1, productManager.getProductCount());
        productManager.removeProduct(product2);
        assertEquals(1, productManager.getProductCount());
        productManager.removeProduct(null);
        assertEquals(1, productManager.getProductCount());
        productManager.removeProduct(product3);
        assertEquals(0, productManager.getProductCount());
    }

    public void testGetProduct() {
        addAllProducts();

        assertSame(product1, productManager.getProduct(0));
        assertSame(product2, productManager.getProduct(1));
        assertSame(product3, productManager.getProduct(2));
    }

    public void testGetProductNames() {
        addAllProducts();

        String[] names = productManager.getProductNames();
        assertEquals(names[0], product1.getName());
        assertEquals(names[1], product2.getName());
        assertEquals(names[2], product3.getName());
    }

    public void testAddProductsWithTheSameName() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        productManager.addProduct(product1);
        productManager.addProduct(product2);
        productManager.addProduct(product3);

        assertEquals(3, productManager.getProductCount());
        assertSame(product1, productManager.getProduct(0));
        assertSame(product2, productManager.getProduct(1));
        assertSame(product3, productManager.getProduct(2));
    }

    public void testGetProductDisplayNames() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        productManager.addProduct(product1);
        productManager.addProduct(product2);
        productManager.addProduct(product3);

        String[] names = productManager.getProductDisplayNames();
        assertEquals(3, names.length);
        assertEquals("[1] name", names[0]);
        assertEquals("[2] name", names[1]);
        assertEquals("[3] name", names[2]);
    }

    public void testGetProductByDisplayName() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        productManager.addProduct(product1);
        productManager.addProduct(product2);
        productManager.addProduct(product3);

        assertEquals(3, productManager.getProductCount());
        assertSame(product1, productManager.getProductByDisplayName("[1] name"));
        assertSame(product2, productManager.getProductByDisplayName("[2] name"));
        assertSame(product3, productManager.getProductByDisplayName("[3] name"));
    }

    public void testVirtualBandExpressionsAreUpdateIfForeignNodeNameChanged() {
        final Product product1 = new Product("P1", "t", 1, 1);
        final VirtualBand p1v1 = new VirtualBand("P1V1", ProductData.TYPE_FLOAT32, 1, 1, "42");
        product1.addBand(p1v1);
        final Product product2 = new Product("P2", "t", 1, 1);
        final VirtualBand p2v1 = new VirtualBand("P2V1", ProductData.TYPE_FLOAT32, 1, 1, "$1.P1V1");
        product2.addBand(p2v1);
        final Product product3 = new Product("P3", "t", 1, 1);
        final VirtualBand p3v1 = new VirtualBand("P3V1", ProductData.TYPE_FLOAT32, 1, 1, "$1.P1V1 + $2.P2V1");
        product3.addBand(p3v1);

        productManager.addProduct(product1);
        productManager.addProduct(product2);
        productManager.addProduct(product3);

        p1v1.setName("TheAnswer");

        assertEquals("$1.TheAnswer", p2v1.getExpression());
        assertEquals("$1.TheAnswer + $2.P2V1", p3v1.getExpression());
    }

    public void testMaskExpressionsAreUpdateIfForeignNodeNameChanged() {
        final Product product1 = new Product("P1", "t", 1, 1);
        final Product product2 = new Product("P2", "t", 1, 1);

        productManager.addProduct(product1);
        productManager.addProduct(product2);

        final VirtualBand p1v1 = new VirtualBand("P1V1", ProductData.TYPE_FLOAT32, 1, 1, "42");
        product1.addBand(p1v1);
        Mask mask = product2.addMask("P2BD", "$1.P1V1 == 42.0", "P2-Bitmask", Color.RED, 0.5f);

        p1v1.setName("TheAnswer");

        assertEquals("$1.TheAnswer == 42.0", Mask.BandMathsType.getExpression(mask));
    }


    private void addAllProducts() {
        productManager.addProduct(product1);
        productManager.addProduct(product2);
        productManager.addProduct(product3);
    }

    private class ProductManagerListener implements ProductManager.Listener {

        private Vector<Product> addedProducts = new Vector<>();
        private Vector<Product> removedProducts = new Vector<>();

        public void productAdded(ProductManager.Event event) {
            addedProducts.add(event.getProduct());
        }

        public void productRemoved(ProductManager.Event event) {
            removedProducts.add(event.getProduct());
        }

        public Vector<Product> getAddedProducts() {
            return addedProducts;
        }

        public Vector<Product> getRemovedProducts() {
            return removedProducts;
        }
    }
}
