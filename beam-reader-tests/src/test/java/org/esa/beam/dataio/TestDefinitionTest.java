package org.esa.beam.dataio;

import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestDefinitionTest {

    private TestDefinition definition;

    @Before
    public void setUp() {
        definition = new TestDefinition();
    }

    @Test
    public void testSetGetProductReaderPlugin() {
        final LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();

        definition.setProductReaderPlugin(readerPlugin);
        assertSame(readerPlugin, definition.getProductReaderPlugin());
    }

    @Test
    public void testGetAllProducts_empty() {
        final List<TestProduct> products = definition.getAllProducts();
        assertNotNull(products);
        assertEquals(0, products.size());
    }

    @Test
    public void testAddListAndGetAll(){
        final TestProduct testProduct = new TestProduct();
        final ArrayList<TestProduct> testProducts = new ArrayList<TestProduct>();
        testProducts.add(testProduct);

        definition.addTestProducts(testProducts);
        final List<TestProduct> allProducts = definition.getAllProducts();
        assertNotNull(allProducts);
        assertEquals(1, allProducts.size());
    }

    @Test
    public void testGetExpectedDatasetById_empty() {
        final ExpectedDataset expectedDataset = definition.getExpectedDataset("an_id");
        assertNull(expectedDataset);
    }

    @Test
    public void testGetExpectedDataset() {
        final ExpectedDataset expectedDataset = new ExpectedDataset();
        expectedDataset.setId("identifier");

        definition.addExpectedDataset(expectedDataset);

        final ExpectedDataset dataset = definition.getExpectedDataset("identifier");
        assertNotNull(dataset);
    }
}
