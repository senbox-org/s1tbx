package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;

abstract class AbstractMetadataAggregator implements MetadataAggregator {

    protected final MetadataElement inputsMetaElement;
    protected int aggregatedCount;

    AbstractMetadataAggregator() {
        aggregatedCount = 0;
        inputsMetaElement = new MetadataElement("sources");
    }
}
