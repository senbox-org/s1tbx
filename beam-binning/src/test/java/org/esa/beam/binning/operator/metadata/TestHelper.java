package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

class TestHelper {

    static void assertCorrectNameAndNoAttributes(MetadataElement metadataElement) {
        assertNotNull(metadataElement);
        assertEquals("source_products", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
    }

    static Product createProduct(int number) {
        return new Product("product_" + Integer.toString(number), "test_type", 2, 2);
    }

    static Product createProductWithProcessingGraph(int number) {
        final Product product = createProduct(number);
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(new MetadataElement("Processing_Graph"));

        return product;
    }

    static void assertProductElementAt(int index, MetadataElement metadataElement) {
        final MetadataElement productElement = metadataElement.getElementAt(index);
        assertEquals("source_product." + Integer.toString(index), productElement.getName());
        final MetadataAttribute nameAttribute = productElement.getAttribute("name");
        assertNotNull(nameAttribute);
        assertEquals("product_" + Integer.toString(index + 1), nameAttribute.getData().getElemString());
    }

    static void assertProductElementWithGraphtAt(int index, MetadataElement metadataElement) {
        assertProductElementAt(index, metadataElement);

        assertNotNull(getProcessingGraphElement(index, metadataElement));
    }

    static void assertProductElementWithoutGraphtAt(int index, MetadataElement metadataElement) {
        assertProductElementAt(index, metadataElement);

        assertNull(getProcessingGraphElement(index, metadataElement));
    }

    private static MetadataElement getProcessingGraphElement(int index, MetadataElement metadataElement) {
        final MetadataElement productElement = metadataElement.getElementAt(index);
        return productElement.getElement("Processing_Graph");
    }
}
