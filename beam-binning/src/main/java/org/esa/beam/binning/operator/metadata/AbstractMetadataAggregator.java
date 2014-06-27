package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;

abstract class AbstractMetadataAggregator implements MetadataAggregator {

    protected final MetadataElement source_products;
    protected int aggregatedCount;

    AbstractMetadataAggregator() {
        aggregatedCount = 0;
        source_products = new MetadataElement("source_products");
    }
}
