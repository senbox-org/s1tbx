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
    public void testAddListAndGetAll() {
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
    public void testGetExpectedDatasetbyId() {
        final ExpectedDataset expectedDataset = new ExpectedDataset();
        expectedDataset.setId("identifier");

        definition.addExpectedDataset(expectedDataset);

        final ExpectedDataset dataset = definition.getExpectedDataset("identifier");
        assertNotNull(dataset);
    }

    @Test
    public void testGetIntendedProductIds_empty() {
        final List<String> expectedIds = definition.getDecodableProductIds();

        assertNotNull(expectedIds);
        assertEquals(0, expectedIds.size());
    }

    @Test
    public void testGetIntendedProductIds() {
        final ExpectedDataset expectedDataset = new ExpectedDataset();
        expectedDataset.setId("identifier");
        expectedDataset.setDecodeQualification("intended");
        definition.addExpectedDataset(expectedDataset);

        final List<String> expectedIds = definition.getDecodableProductIds();

        assertEquals(1, expectedIds.size());
    }

    @Test
    public void testGetIntendedProductIds_mixed() {
        final ExpectedDataset suitableDataset = new ExpectedDataset();
        suitableDataset.setId("id_suitable");
        suitableDataset.setDecodeQualification("suitable");
        definition.addExpectedDataset(suitableDataset);

        final ExpectedDataset intendedDataset = new ExpectedDataset();
        intendedDataset.setId("id_intended");
        intendedDataset.setDecodeQualification("intended");
        definition.addExpectedDataset(intendedDataset);

        final List<String> expectedIds = definition.getDecodableProductIds();

        assertEquals(2, expectedIds.size());
        assertEquals("id_suitable", expectedIds.get(0));
        assertEquals("id_intended", expectedIds.get(1));
    }
}
