package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

public interface MetadataAggregator {

    void aggregateMetadata(Product product);

    void aggregateMetadata(MetadataElement processingGraphElement);

    MetadataElement getMetadata();
}
