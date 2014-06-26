package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProductNameMetaAggregatorTest {

    private ProductNameMetaAggregator aggregator;

    @Before
    public void setUp() {
        aggregator = new ProductNameMetaAggregator();
    }

    @Test
    public void testAggregateNoProducts() {
        final MetadataElement metadataElement = aggregator.getMetadata();
        assertCorrectNameAndNoAttributes(metadataElement);
        assertEquals(0, metadataElement.getNumElements());
    }

    @Test
    public void testAggregateOneProduct() {
        final Product product = createProduct(1);

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        assertProductElementAt(metadataElement, 0);
    }

    @Test
    public void testAggregateThreeProducts() {
        Product product = createProduct(1);
        aggregator.aggregateMetadata(product);

        product = createProduct(2);
        aggregator.aggregateMetadata(product);

        product = createProduct(3);
        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(3, metadataElement.getNumElements());
        assertProductElementAt(metadataElement, 0);
        assertProductElementAt(metadataElement, 1);
        assertProductElementAt(metadataElement, 2);
    }

    private void assertProductElementAt(MetadataElement metadataElement, int index) {
        final MetadataElement productElement = metadataElement.getElementAt(index);
        assertEquals("source_product." + Integer.toString(index), productElement.getName());
        final MetadataAttribute nameAttribute = productElement.getAttribute("name");
        assertNotNull(nameAttribute);
        assertEquals("product_" + Integer.toString(index + 1), nameAttribute.getData().getElemString());
    }

    private Product createProduct(int number) {
        return new Product("product_" + Integer.toString(number), "test_type", 2, 2);
    }

    private void assertCorrectNameAndNoAttributes(MetadataElement metadataElement) {
        assertNotNull(metadataElement);
        assertEquals("source_products", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
    }
}
