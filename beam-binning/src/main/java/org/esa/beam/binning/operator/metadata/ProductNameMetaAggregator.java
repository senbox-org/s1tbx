package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class ProductNameMetaAggregator extends AbstractMetadataAggregator {

//    private final MetadataElement source_products;
//    private int aggregatedCount;
//
//    ProductNameMetaAggregator() {
//        source_products = new MetadataElement("source_products");
//        aggregatedCount = 0;
//    }

    public MetadataElement getMetadata() {
        return source_products;
    }

    public void aggregateMetadata(Product product) {
        final MetadataElement productElement = Utilities.createProductMetaElement(product, aggregatedCount);

        source_products.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }
}
