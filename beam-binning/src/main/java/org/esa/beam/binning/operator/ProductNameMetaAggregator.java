package org.esa.beam.binning.operator;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class ProductNameMetaAggregator {

    MetadataElement getMetadata() {
        return new MetadataElement("source_products");
    }

    void aggregateMetadata(Product product) {

    }
}
