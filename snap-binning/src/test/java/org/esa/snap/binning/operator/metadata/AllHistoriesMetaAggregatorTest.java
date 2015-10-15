package org.esa.snap.binning.operator.metadata;


import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
        TestHelper.assertInputElementAt(0, metadataElement);
    }

    @Test
    public void testAggregateOneProductWithProcessingInfo() {
        final Product product = TestHelper.createProductWithProcessingGraph(1, "schnickschnack");

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphAt(0, "schnickschnack", metadataElement);
    }

    @Test
    public void testAggregateThreeProductMixed() {
        Product product = TestHelper.createProductWithProcessingGraph(1, "one");
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProduct(2);
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProductWithProcessingGraph(3, "three");
        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(3, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphAt(0, "one", metadataElement);
        TestHelper.assertProductElementWithoutGraphtAt(1, "product_2", metadataElement);
        TestHelper.assertProductElementWithGraphAt(2, "three", metadataElement);
    }

    // @todo 3 tb/tb add tests for aggregation on metadataElement 2014-10-15
}
