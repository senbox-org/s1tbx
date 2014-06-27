package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

class Utilities {

    static MetadataElement createProductMetaElement(Product product, int index) {
        final MetadataElement productElement = new MetadataElement("source_product." + Integer.toString(index));
        final MetadataAttribute nameAttribute = new MetadataAttribute("name", new ProductData.ASCII(product.getName()), true);
        productElement.addAttribute(nameAttribute);
        return productElement;
    }

    static MetadataElement getProcessingGraphElement(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        return metadataRoot.getElement("Processing_Graph");
    }
}
