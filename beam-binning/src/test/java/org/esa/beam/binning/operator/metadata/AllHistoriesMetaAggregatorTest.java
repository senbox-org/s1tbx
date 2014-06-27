package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AllHistoriesMetaAggregatorTest {

    private AllHistoriesMetaAggregator aggregator;

    @Before
    public void setUp() {
        aggregator = new AllHistoriesMetaAggregator();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testInterfaceImplemented() {
        assertTrue(aggregator instanceof MetadataAggregator);
    }

    @Test
    public void testAggregateNoProducts() {
        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);
        assertEquals(0, metadataElement.getNumElements());
    }

    @Test
    public void testAggregateOneProductWithoutProcessingInfo() {
        final Product product = TestHelper.createProduct(1);

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertProductElementAt(0, metadataElement);
    }

    @Test
     public void testAggregateOneProductWithProcessingInfo() {
        final Product product = TestHelper.createProductWithProcessingGraph(1);

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphtAt(0, metadataElement);
    }

    @Test
    public void testAggregateThreeProductMixed() {
        Product product = TestHelper.createProductWithProcessingGraph(1);
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProduct(2);
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProductWithProcessingGraph(3);
        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(3, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphtAt(0, metadataElement);
        TestHelper.assertProductElementWithoutGraphtAt(1, metadataElement);
        TestHelper.assertProductElementWithGraphtAt(2, metadataElement);
    }
}
