package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

class ProductNameMetaAggregator extends AbstractMetadataAggregator {

    public MetadataElement getMetadata() {
        return inputsMetaElement;
    }

    public void aggregateMetadata(Product product) {
        final String productName = Utilities.extractProductName(product);

        aggregate(productName);
    }

    @Override
    public void aggregateMetadata(MetadataElement processingGraphElement) {
        String productName = Utilities.extractProductName(processingGraphElement);
        if (StringUtils.isNullOrEmpty(productName)) {
            productName = "unknown";
        }

        aggregate(productName);
    }

    private void aggregate(String productName) {
        final MetadataElement productElement = Utilities.createSourceMetaElement(productName, aggregatedCount);

        inputsMetaElement.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }
}
