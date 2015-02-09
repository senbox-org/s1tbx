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

package org.esa.beam.framework.datamodel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.awt.Color;
import java.util.Vector;

public class ProductManagerTest extends TestCase {

    private static final String _prodName = "TestProduct";
    private static final int _sceneWidth = 400;
    private static final int _sceneHeight = 300;

    private ProductManager _productManager;
    private Product _product1;
    private Product _product2;
    private Product _product3;

    public ProductManagerTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductManagerTest.class);
    }

    /**
     * Initializytion for the tests.
     */
    @Override
    protected void setUp() {
        _productManager = new ProductManager();
        _product1 = new Product("product1", _prodName, _sceneWidth, _sceneHeight);
        _product2 = new Product("product2", _prodName, _sceneWidth, _sceneHeight);
        _product3 = new Product("product3", _prodName, _sceneWidth, _sceneHeight);
    }

    @Override
    protected void tearDown() {

    }

    public void testAddProduct() {
        final ProductManagerListener listener = new ProductManagerListener();
        _productManager.addListener(listener);

        _productManager.addProduct(_product1);

        assertEquals(1, _productManager.getProductCount());
        assertSame(_product1, _productManager.getProduct(0));
        assertSame(_product1, _productManager.getProduct("product1"));
        assertEquals(1, _product1.getRefNo());
        assertSame(_productManager, _product1.getProductManager());

        final Vector addedProducts = listener.getAddedProducts();
        assertEquals(1, addedProducts.size());
        assertSame(_product1, addedProducts.get(0));

        final Vector removedProducts = listener.getRemovedProducts();
        assertEquals(0, removedProducts.size());
    }

    public void testRemoveProduct() {
        addAllProducts();

        final ProductManagerListener listener = new ProductManagerListener();
        _productManager.addListener(listener);

        _productManager.removeProduct(_product2);

        assertEquals(2, _productManager.getProductCount());
        assertSame(_product1, _productManager.getProduct(0));
        assertSame(_product3, _productManager.getProduct(1));
        assertSame(_product1, _productManager.getProduct("product1"));
        assertNull(_productManager.getProduct("product2"));
        assertSame(_product3, _productManager.getProduct("product3"));
        assertEquals(1, _product1.getRefNo());
        assertEquals(0, _product2.getRefNo());
        assertEquals(3, _product3.getRefNo());
        assertSame(_productManager, _product1.getProductManager());
        assertNull(_product2.getProductManager());
        assertSame(_productManager, _product3.getProductManager());

        final Vector addedProducts = listener.getAddedProducts();
        assertEquals(0, addedProducts.size());

        final Vector removedProducts = listener.getRemovedProducts();
        assertEquals(1, removedProducts.size());
        assertSame(_product2, removedProducts.get(0));
    }

    public void testRemoveAll() {
        addAllProducts();
        final ProductManagerListener listener = new ProductManagerListener();
        _productManager.addListener(listener);
        _productManager.removeAllProducts();

        assertEquals(0, _productManager.getProductCount());

        assertNull(_product1.getProductManager());
        assertNull(_product2.getProductManager());
        assertNull(_product3.getProductManager());

        assertEquals(0, _product1.getRefNo());
        assertEquals(0, _product2.getRefNo());
        assertEquals(0, _product3.getRefNo());


        final Vector removedProducts = listener.getRemovedProducts();
        assertEquals(3, removedProducts.size());
        assertSame(_product1, removedProducts.get(0));
        assertSame(_product2, removedProducts.get(1));
        assertSame(_product3, removedProducts.get(2));

        final Vector addedProducts = listener.getAddedProducts();
        assertEquals(0, addedProducts.size());
    }

    public void testContainsProduct() {
        assertEquals(false, _productManager.containsProduct("product2"));

        _productManager.addProduct(_product2);
        assertEquals(true, _productManager.containsProduct("product2"));

        _productManager.removeProduct(_product2);
        assertEquals(false, _productManager.containsProduct("product2"));
    }

    public void testGetNumProducts() {
        assertEquals(0, _productManager.getProductCount());
        addAllProducts();
        assertEquals(3, _productManager.getProductCount());
        _productManager.removeProduct(_product1);
        assertEquals(2, _productManager.getProductCount());
        _productManager.removeProduct(_product2);
        assertEquals(1, _productManager.getProductCount());
        _productManager.removeProduct(_product2);
        assertEquals(1, _productManager.getProductCount());
        _productManager.removeProduct(null);
        assertEquals(1, _productManager.getProductCount());
        _productManager.removeProduct(_product3);
        assertEquals(0, _productManager.getProductCount());
    }

    public void testGetProduct() {
        addAllProducts();

        assertSame(_product1, _productManager.getProduct(0));
        assertSame(_product2, _productManager.getProduct(1));
        assertSame(_product3, _productManager.getProduct(2));
    }

    public void testGetProductNames() {
        addAllProducts();

        String[] names = _productManager.getProductNames();
        assertEquals(names[0], _product1.getName());
        assertEquals(names[1], _product2.getName());
        assertEquals(names[2], _product3.getName());
    }

    public void testAddProductsWithTheSameName() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        _productManager.addProduct(product1);
        _productManager.addProduct(product2);
        _productManager.addProduct(product3);

        assertEquals(3, _productManager.getProductCount());
        assertSame(product1, _productManager.getProduct(0));
        assertSame(product2, _productManager.getProduct(1));
        assertSame(product3, _productManager.getProduct(2));
    }

    public void testGetProductDisplayNames() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        _productManager.addProduct(product1);
        _productManager.addProduct(product2);
        _productManager.addProduct(product3);

        String[] names = _productManager.getProductDisplayNames();
        assertEquals(3, names.length);
        assertEquals("[1] name", names[0]);
        assertEquals("[2] name", names[1]);
        assertEquals("[3] name", names[2]);
    }

    public void testGetProductByDisplayName() {
        final Product product1 = new Product("name", "t", 1, 1);
        final Product product2 = new Product("name", "t", 1, 1);
        final Product product3 = new Product("name", "t", 1, 1);

        _productManager.addProduct(product1);
        _productManager.addProduct(product2);
        _productManager.addProduct(product3);

        assertEquals(3, _productManager.getProductCount());
        assertSame(product1, _productManager.getProductByDisplayName("[1] name"));
        assertSame(product2, _productManager.getProductByDisplayName("[2] name"));
        assertSame(product3, _productManager.getProductByDisplayName("[3] name"));
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

        _productManager.addProduct(product1);
        _productManager.addProduct(product2);
        _productManager.addProduct(product3);

        p1v1.setName("TheAnswer");

        assertEquals("$1.TheAnswer", p2v1.getExpression());
        assertEquals("$1.TheAnswer + $2.P2V1", p3v1.getExpression());
    }

    public void testBitmaskDefExpressionsAreUpdateIfForeignNodeNameChanged() {
        final Product product1 = new Product("P1", "t", 1, 1);
        final VirtualBand p1v1 = new VirtualBand("P1V1", ProductData.TYPE_FLOAT32, 1, 1, "42");
        product1.addBand(p1v1);
        final Product product2 = new Product("P2", "t", 1, 1);
        final BitmaskDef p2bd = new BitmaskDef("P2BD", "P2-Bitmask", "$1.P1V1 == 42.0", Color.RED, 0.5f);
        product2.addBitmaskDef(p2bd);

        _productManager.addProduct(product1);
        _productManager.addProduct(product2);

        p1v1.setName("TheAnswer");

        assertEquals("$1.TheAnswer == 42.0", p2bd.getExpr());
    }


    private void addAllProducts() {
        _productManager.addProduct(_product1);
        _productManager.addProduct(_product2);
        _productManager.addProduct(_product3);
    }

    private class ProductManagerListener implements ProductManager.Listener {

        private Vector _addedProducts = new Vector();
        private Vector _removedProducts = new Vector();

        public void productAdded(ProductManager.Event event) {
            _addedProducts.add(event.getProduct());
        }

        public void productRemoved(ProductManager.Event event) {
            _removedProducts.add(event.getProduct());
        }

        public Vector getAddedProducts() {
            return _addedProducts;
        }

        public Vector getRemovedProducts() {
            return _removedProducts;
        }
    }
}
