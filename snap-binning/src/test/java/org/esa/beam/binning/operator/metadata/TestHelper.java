package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import static org.junit.Assert.*;

class TestHelper {

    static void assertCorrectNameAndNoAttributes(MetadataElement metadataElement) {
        assertNotNull(metadataElement);
        assertEquals("sources", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
    }

    static Product createProduct(int number) {
        return new Product("product_" + Integer.toString(number), "test_type", 2, 2);
    }

    static Product createProductWithProcessingGraph(int number, String... inputNames) {
        final Product product = createProduct(number);
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement processingGraphElement = new MetadataElement("Processing_Graph");
        if (inputNames != null && inputNames.length > 0) {
            int index = 0;
            for (final String inputName : inputNames) {
                final MetadataElement nodeElement = new MetadataElement("node." + Integer.toString(index));
                final MetadataElement sourcesElement = new MetadataElement("sources");
                sourcesElement.addAttribute(new MetadataAttribute("sourceProduct", ProductData.createInstance(inputName), true));
                nodeElement.addElement(sourcesElement);
                processingGraphElement.addElement(nodeElement);
                ++index;
            }
        }
        metadataRoot.addElement(processingGraphElement);

        return product;
    }

    static void assertInputElementAt(int index, MetadataElement metadataElement) {
        assertInputElementAt(index, "product_" + Integer.toString(index + 1), metadataElement);
    }

    static void assertInputElementAt(int index, String inputName, MetadataElement metadataElement) {
        final MetadataElement productElement = metadataElement.getElementAt(index);
        assertEquals("source." + Integer.toString(index), productElement.getName());

        final MetadataAttribute nameAttribute = productElement.getAttribute("name");
        assertNotNull(nameAttribute);
        assertEquals(inputName, nameAttribute.getData().getElemString());
    }

    static void assertProductElementWithGraphAt(int index, String inputName, MetadataElement metadataElement) {
        assertInputElementAt(index, inputName, metadataElement);

        assertNotNull(getProcessingGraphElement(index, metadataElement));
    }

    static void assertProductElementWithoutGraphtAt(int index, String inputName, MetadataElement metadataElement) {
        assertInputElementAt(index, inputName, metadataElement);

        assertNull(getProcessingGraphElement(index, metadataElement));
    }

    private static MetadataElement getProcessingGraphElement(int index, MetadataElement metadataElement) {
        final MetadataElement productElement = metadataElement.getElementAt(index);
        return productElement.getElement("Processing_Graph");
    }
}
