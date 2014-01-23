package org.esa.beam.dataio;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProductListTest {

    private ProductList productList;

    @Before
    public void setUp() {
        productList = new ProductList();
    }

    @Test
    public void testGetAll_empty() {
        final List<TestProduct> testProducts = productList.getAll();
        assertNotNull(testProducts);
        assertEquals(0, testProducts.size());
    }

    @Test
    public void testGetAll() {
        productList.add(new TestProduct());

        final List<TestProduct> testProducts = productList.getAll();
        assertEquals(1, testProducts.size());
    }

    @Test
    public void testGetAllIds_empty() {
        final String[] ids = productList.getAllIds();
        assertNotNull(ids);
        assertEquals(0, ids.length);
    }

    @Test
    public void testGetAllIds() {
        final TestProduct prod_1 = new TestProduct();
        prod_1.setId("prod_1");

        final TestProduct prod_2 = new TestProduct();
        prod_2.setId("prod_2");

        productList.add(prod_1);
        productList.add(prod_2);

        final String[] ids = productList.getAllIds();
        assertEquals(2, ids.length);
        assertEquals("prod_1", ids[0]);
        assertEquals("prod_2", ids[1]);
    }
}
