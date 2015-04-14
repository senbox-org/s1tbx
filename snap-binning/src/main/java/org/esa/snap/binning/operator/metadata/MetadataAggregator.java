package org.esa.snap.binning.operator.metadata;


import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;

public interface MetadataAggregator {

    void aggregateMetadata(Product product);

    void aggregateMetadata(MetadataElement processingGraphElement);

    MetadataElement getMetadata();
}
