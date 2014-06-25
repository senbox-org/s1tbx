package org.esa.beam.binning.operator;

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
        assertNotNull(metadataElement);
        assertEquals("source_products", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
        assertEquals(0, metadataElement.getNumElements());
    }

    @Test
    public void testAggregateOneProduct() {
        final Product product = new Product("product_1", "test_type", 2, 2);

        aggregator.aggregateMetadata(product);
        // @todo 1 tb/tb continue here 2014-06-25
    }
}
