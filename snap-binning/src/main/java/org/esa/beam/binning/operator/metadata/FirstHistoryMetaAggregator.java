package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StringUtils;

class FirstHistoryMetaAggregator extends AbstractMetadataAggregator {

    @Override
    public void aggregateMetadata(Product product) {
        final String productName = Utilities.extractProductName(product);
        final MetadataElement processingGraphElement = Utilities.getProcessingGraphElement(product);

        aggregate(productName, processingGraphElement);
    }

    @Override
    public void aggregateMetadata(MetadataElement processingGraphElement) {
        String productName = Utilities.extractProductName(processingGraphElement);
        if (StringUtils.isNullOrEmpty(productName)) {
            productName = "unknown";
        }

        aggregate(productName, processingGraphElement);

    }

    @Override
    public MetadataElement getMetadata() {
        return inputsMetaElement;
    }

    private void aggregate(String productName, MetadataElement processingGraphElement) {
        final MetadataElement inputMetaElement = Utilities.createSourceMetaElement(productName, aggregatedCount);
        if (processingGraphElement != null && aggregatedCount == 0) {
            inputMetaElement.addElement(processingGraphElement.createDeepClone());
        }

        inputsMetaElement.addElementAt(inputMetaElement, aggregatedCount);
        ++aggregatedCount;
    }
}
