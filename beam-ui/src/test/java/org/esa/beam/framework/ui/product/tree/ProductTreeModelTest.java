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

package org.esa.beam.framework.ui.product.tree;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductTreeModelTest {

    private ProductManager productManager;
    private ProductTreeModel treeModel;


    @Before
    public void setupTest() {
        productManager = new ProductManager();
        treeModel = new ProductTreeModel(productManager);
    }

    @Test
    public void testAutoGrouping() {

        final Product product = new Product("A", "B", 10, 10);
        product.addBand("chl_20100412T152322", ProductData.TYPE_FLOAT32);
        product.addBand("chl_20100412T152443", ProductData.TYPE_FLOAT32);
        product.addBand("chl_20100412T152512", ProductData.TYPE_FLOAT32);
        product.addBand("chl_20100412T152615", ProductData.TYPE_FLOAT32);
        product.addBand("chl_20100412T152715", ProductData.TYPE_FLOAT32);
        product.addBand("tsm_20100412T152322", ProductData.TYPE_FLOAT32);
        product.addBand("tsm_20100412T152443", ProductData.TYPE_FLOAT32);
        product.addBand("tsm_20100412T152512", ProductData.TYPE_FLOAT32);
        product.addBand("tsm_20100412T152615", ProductData.TYPE_FLOAT32);
        product.addBand("tsm_20100412T152715", ProductData.TYPE_FLOAT32);
        product.addBand("a_flags", ProductData.TYPE_INT16);
        product.addBand("b_flags", ProductData.TYPE_INT16);

        product.setAutoGrouping("");
        productManager.addProduct(product);
        assertEquals(1, treeModel.getRoot().getChildCount());
        assertEquals(1, treeModel.getRoot().getChildAt(0).getChildCount());
        assertEquals(12, treeModel.getRoot().getChildAt(0).getChildAt(0).getChildCount());

        productManager.removeProduct(product);

        product.setAutoGrouping("chl:tsm");
        productManager.addProduct(product);
        assertEquals(1, treeModel.getRoot().getChildCount());
        assertEquals(1, treeModel.getRoot().getChildAt(0).getChildCount());
        assertEquals(4, treeModel.getRoot().getChildAt(0).getChildAt(0).getChildCount());
        assertEquals("chl", treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(0).getName());
        assertEquals(5, treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(0).getChildCount());
        assertEquals("tsm", treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(1).getName());
        assertEquals(5, treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(1).getChildCount());
        assertEquals("a_flags", treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(2).getName());
        assertEquals("b_flags", treeModel.getRoot().getChildAt(0).getChildAt(0).getChildAt(3).getName());
    }

    @Test
    public void testEmptyManager() {
        assertSame(productManager, treeModel.getProductManager());
        final ProductManagerTN treeRoot = (ProductManagerTN) treeModel.getRoot();
        assertSame(productManager, treeRoot.getContent());
        assertTrue("Root is not a leaf", treeModel.isLeaf(treeRoot));
        assertEquals(0, treeModel.getChildCount(treeRoot));
    }

    @Test
    public void testAddingRemovingProducts() {
        final Object treeRoot = treeModel.getRoot();
        final Product product1 = createDummyProduct("x1");
        productManager.addProduct(product1);

        assertEquals(1, treeModel.getChildCount(treeRoot));
        final ProductTN child1 = (ProductTN) treeModel.getChild(treeRoot, 0);
        assertSame(product1, child1.getProduct());

        final Product product2 = createDummyProduct("x2");
        productManager.addProduct(product2);

        assertEquals(2, treeModel.getChildCount(treeRoot));
        final ProductTN child2 = (ProductTN) treeModel.getChild(treeRoot, 1);
        assertSame(product2, child2.getProduct());

        productManager.removeProduct(product1);

        assertEquals(1, treeModel.getChildCount(treeRoot));
        final ProductTN child3 = (ProductTN) treeModel.getChild(treeRoot, 0);
        assertSame(product2, child3.getProduct());

    }

    @Test
    public void testAddingRemovingProductNodeGroups() {
        final Product product = createDummyProduct("x1");
        productManager.addProduct(product);
        AbstractTN rootNode = treeModel.getRoot();
        ProductTN productTN = (ProductTN) treeModel.getChild(rootNode, 0);
        assertEquals(0, treeModel.getChildCount(productTN));

        // 1 node: "Bands"
        product.addBand("b1", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productTN));
        product.addBand("b2", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productTN));

        // 2 nodes now: "Bands" and "Vector Data" (with "Pins")
        final Placemark pin1 = createDummyPin("p1");
        product.getPinGroup().add(pin1);
        assertEquals(2, treeModel.getChildCount(productTN));
        // Still 2 nodes: "Bands" and "Vector Data" (with "Pins", but "v1" is empty)
        final VectorDataNode vec1 = new VectorDataNode("v1", PlainFeatureFactory.createDefaultFeatureType());
        product.getVectorDataGroup().add(vec1);
        assertEquals(2, treeModel.getChildCount(productTN));
        // When we remove "pin1", "Vector Data" goes away, because "Pins" is now empty
        product.getPinGroup().remove(pin1);
        assertEquals(1, treeModel.getChildCount(productTN));
        // Removing "vec1" should not change current state
        product.getVectorDataGroup().remove(vec1);
        assertEquals(1, treeModel.getChildCount(productTN));
    }

    @Test
    public void testAddingRemovingProductNodes() {
        final Product product = createDummyProduct("x1");
        productManager.addProduct(product);
        AbstractTN rootNode = treeModel.getRoot();
        ProductTN productTN = (ProductTN) treeModel.getChild(rootNode, 0);

        assertEquals(0, treeModel.getChildCount(productTN));

        final Band band1 = product.addBand("b1", ProductData.TYPE_INT8);
        assertEquals(1, treeModel.getChildCount(productTN));
        final Object bandGroup = treeModel.getChild(productTN, 0);
        assertNotNull(bandGroup);
        assertEquals(1, treeModel.getChildCount(bandGroup));
        assertSame(band1, ((ProductNodeTN) treeModel.getChild(bandGroup, 0)).getProductNode());

        product.addBand("b2", ProductData.TYPE_INT8);
        assertEquals(2, treeModel.getChildCount(bandGroup));
        product.removeBand(product.getBand("b2"));
        product.removeBand(product.getBand("b1"));
        assertEquals(0, treeModel.getChildCount(productTN));
    }


    private Placemark createDummyPin(String name) {
        return Placemark.createPointPlacemark(PinDescriptor.getInstance(), name, "", "",
                                              new PixelPos(0.5f, 0.5f), null,
                                              null);
    }

    private Product createDummyProduct(String name) {
        return new Product(name, "t", 1, 1);
    }

}
