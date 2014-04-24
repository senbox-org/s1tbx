package org.esa.beam.binning.operator.ui;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.binning.aggregators.AggregatorAverage;

/**
* @author Norman Fomferra
*/
class AggregatorItem {

    AggregatorDescriptor aggregatorDescriptor;
    AggregatorConfig aggregatorConfig;

    AggregatorItem() {
        this.aggregatorDescriptor = new AggregatorAverage.Descriptor();
        this.aggregatorConfig = aggregatorDescriptor.createConfig();
    }

    AggregatorItem(AggregatorConfig aggregatorConfig) {
        this.aggregatorConfig = aggregatorConfig;
        this.aggregatorDescriptor = TypedDescriptorsRegistry.getInstance().getDescriptor(AggregatorDescriptor.class, aggregatorConfig.getName());
    }
}
