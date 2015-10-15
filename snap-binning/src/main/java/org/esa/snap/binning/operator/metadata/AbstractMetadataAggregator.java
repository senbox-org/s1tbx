package org.esa.snap.binning.operator.metadata;

import org.esa.snap.core.datamodel.MetadataElement;

abstract class AbstractMetadataAggregator implements MetadataAggregator {

    protected final MetadataElement inputsMetaElement;
    protected int aggregatedCount;

    AbstractMetadataAggregator() {
        aggregatedCount = 0;
        inputsMetaElement = new MetadataElement("sources");
    }
}
