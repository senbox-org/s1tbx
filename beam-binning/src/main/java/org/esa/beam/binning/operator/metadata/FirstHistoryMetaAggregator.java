package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class FirstHistoryMetaAggregator implements MetadataAggregator {

    private final MetadataElement source_products;
    private int aggregatedCount;

    FirstHistoryMetaAggregator() {
        source_products = new MetadataElement("source_products");
        aggregatedCount = 0;
    }

    @Override
    public void aggregateMetadata(Product product) {
        final MetadataElement productElement = Utilities.createProductMetaElement(product, aggregatedCount);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement processingGraphElement = metadataRoot.getElement("Processing_Graph");
        if (processingGraphElement != null && aggregatedCount == 0) {
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
