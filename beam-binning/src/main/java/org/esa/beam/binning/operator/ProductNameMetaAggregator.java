package org.esa.beam.binning.operator;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

class ProductNameMetaAggregator {

    private final MetadataElement source_products;
    private int aggregatedCount;

    ProductNameMetaAggregator() {
        source_products = new MetadataElement("source_products");
        aggregatedCount = 0;
    }

    MetadataElement getMetadata() {
        return source_products;
    }

    void aggregateMetadata(Product product) {
        final MetadataElement productElement = new MetadataElement("source_product." + Integer.toString(aggregatedCount));
        final MetadataAttribute nameAttribute = new MetadataAttribute("name", new ProductData.ASCII(product.getName()), true);
        productElement.addAttribute(nameAttribute);

        source_products.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }
}
