/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import org.esa.beam.HeadlessTestRunner;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@RunWith(HeadlessTestRunner.class)
public class SourceProductSelectorTest {

    private Product[] defaultProducts;
    private DefaultAppContext appContext;

    @Before
    public void setUp() throws Exception {
        appContext = new DefaultAppContext("Fart, fart!");
        defaultProducts = new Product[4];
        for (int i = 0; i < defaultProducts.length; i++) {
            defaultProducts[i] = new Product("P" + i, "T" + i, 10, 10);
            appContext.getProductManager().addProduct(defaultProducts[i]);
        }
        appContext.setSelectedProduct(defaultProducts[0]);
    }

    @Test
    public void testCreatedUIComponentsNotNull() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source:");
        selector.initProducts();
        Assert.assertNotNull(selector.getProductNameLabel());
        Assert.assertNotNull(selector.getProductNameComboBox());
        Assert.assertNotNull(selector.getProductFileChooserButton());
    }

    @Test
    public void testCreatedUIComponentsAreSame() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source:");
        selector.initProducts();
        Assert.assertSame(selector.getProductNameLabel(), selector.getProductNameLabel());
        Assert.assertSame(selector.getProductNameComboBox(), selector.getProductNameComboBox());
        Assert.assertSame(selector.getProductFileChooserButton(), selector.getProductFileChooserButton());
    }

    @Test
    public void testSetSelectedProduct() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        Product selectedProduct = selector.getSelectedProduct();
        Assert.assertSame(appContext.getSelectedProduct(), selectedProduct);

        selector.setSelectedProduct(defaultProducts[1]);
        selectedProduct = selector.getSelectedProduct();
        Assert.assertSame(defaultProducts[1], selectedProduct);

        Product oldProduct = new Product("new", "T1", 0, 0);
        oldProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(oldProduct);
        selectedProduct = selector.getSelectedProduct();
        Assert.assertSame(oldProduct, selectedProduct);

        Product newProduct = new Product("new", "T2", 0, 0);
        selector.setSelectedProduct(newProduct);
        selectedProduct = selector.getSelectedProduct();
        Assert.assertSame(newProduct, selectedProduct);
        Assert.assertNull(oldProduct.getFileLocation()); // assert that old product is disposed
    }

    @Test
    public void testSelectedProductIsRemoved() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        appContext.getProductManager().removeProduct(defaultProducts[0]);
        Assert.assertEquals(defaultProducts.length - 1, selector.getProductCount());
    }

    @Test
    public void testNotSelectedProductIsRemoved() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        appContext.getProductManager().removeProduct(defaultProducts[2]);
        Assert.assertEquals(defaultProducts.length - 1, selector.getProductCount());
    }

    @Test
    public void testNewProductIsDisposed() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);
        Assert.assertSame(newProduct, selector.getSelectedProduct());
        selector.setSelectedProduct(defaultProducts[0]);
        Assert.assertSame(defaultProducts[0], selector.getSelectedProduct());

        Assert.assertNotNull(newProduct.getFileLocation());
        selector.releaseProducts();
        Assert.assertNull(newProduct.getFileLocation()); // assert that new product is disposed, because it is not selected
    }

    @Test
    public void testNewProductIsNotDisposed() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        selector.setSelectedProduct(defaultProducts[0]);
        Assert.assertSame(defaultProducts[0], selector.getSelectedProduct());
        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);
        Assert.assertSame(newProduct, selector.getSelectedProduct());

        Assert.assertNotNull(newProduct.getFileLocation());
        selector.releaseProducts();
        Assert.assertNotNull(newProduct.getFileLocation()); // assert that new product is not disposed while it is selected
    }

    @Test
    public void testSetSelectedIndex() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");

        selector.initProducts();
        Assert.assertSame(defaultProducts[0], selector.getSelectedProduct());

        selector.setSelectedIndex(1);
        Assert.assertSame(defaultProducts[1], selector.getSelectedProduct());

        selector.setSelectedIndex(2);
        Assert.assertSame(defaultProducts[2], selector.getSelectedProduct());
    }
}
