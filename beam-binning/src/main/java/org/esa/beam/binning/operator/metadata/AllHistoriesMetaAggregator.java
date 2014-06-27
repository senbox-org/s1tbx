package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class AllHistoriesMetaAggregator extends AbstractMetadataAggregator {

    @Override
    public void aggregateMetadata(Product product) {
        final MetadataElement productElement = Utilities.createProductMetaElement(product, aggregatedCount);

        final MetadataElement processingGraphElement = Utilities.getProcessingGraphElement(product);
        if (processingGraphElement != null) {
            productElement.addElement(processingGraphElement.createDeepClone());
        }
        source_products.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }

    @Override
    public MetadataElement getMetadata() {
        return source_products;
    }
}
