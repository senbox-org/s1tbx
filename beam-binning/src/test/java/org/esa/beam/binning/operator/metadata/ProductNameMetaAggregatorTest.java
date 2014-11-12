package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductNameMetaAggregatorTest {

    private ProductNameMetaAggregator aggregator;

    @Before
    public void setUp() {
        aggregator = new ProductNameMetaAggregator();
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
    public void testAggregateOneInput_withoutGraph() {
        final Product product = TestHelper.createProduct(1);

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertInputElementAt(0, metadataElement);
    }

    @Test
    public void testAggregateThreeInputs_withoutGraph() {
        Product product = TestHelper.createProduct(1);
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProduct(2);
        aggregator.aggregateMetadata(product);

        product = TestHelper.createProduct(3);
        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(3, metadataElement.getNumElements());
        TestHelper.assertInputElementAt(0, metadataElement);
        TestHelper.assertInputElementAt(1, metadataElement);
        TestHelper.assertInputElementAt(2, metadataElement);
    }

    @Test
    public void testAggregateOneInput_withGraph() {
        final Product product = TestHelper.createProductWithProcessingGraph(1, "inputProduct");

        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertInputElementAt(0, "inputProduct", metadataElement);
    }

    @Test
    public void testAggregateOneInput_withGraph_withoutValidProductName() {
        final Product product = TestHelper.createProductWithProcessingGraph(1, "inputProduct");

        final MetadataElement node_0 = product.getMetadataRoot().getElement("Processing_Graph").getElement("node.0");
        final MetadataElement sourcesElement = node_0.getElement("sources");
        final MetadataAttribute sourceProductAttribute = sourcesElement.getAttribute("sourceProduct");
        sourcesElement.removeAttribute(sourceProductAttribute);


        aggregator.aggregateMetadata(product);

        final MetadataElement metadataElement = aggregator.getMetadata();
        TestHelper.assertCorrectNameAndNoAttributes(metadataElement);

        assertEquals(1, metadataElement.getNumElements());
        TestHelper.assertInputElementAt(0, "product_1", metadataElement);
    }

    // @todo 3 tb/tb add tests for aggregation on metadataElement 2014-10-15
}
