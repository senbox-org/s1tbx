package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FirstHistoryMetaAggregatorTest {

    private FirstHistoryMetaAggregator aggregator;

    @Before
    public void setUp() {
        aggregator = new FirstHistoryMetaAggregator();
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
        final Product product = TestHelper.createProductWithProcessingGraph(1, "a-test_1", "a_test_2");

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphAt(0, "a-test_1", metadataElement);
    }

    @Test
    public void testAggregateThreeProductWithProcessingInfo() {
        Product product = TestHelper.createProductWithProcessingGraph(1, "first input");
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProductWithProcessingGraph(2, "second input");
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProductWithProcessingGraph(3, "third input");
        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(3, metadataElement.getNumElements());
        TestHelper.assertProductElementWithGraphAt(0, "first input", metadataElement);
        TestHelper.assertProductElementWithoutGraphtAt(1, "second input", metadataElement);
        TestHelper.assertProductElementWithoutGraphtAt(2, "third input", metadataElement);
    }

    // @todo 3 tb/tb add tests for aggregation on metadataElement 2014-10-15
}
