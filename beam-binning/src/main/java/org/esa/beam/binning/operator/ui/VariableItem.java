package org.esa.beam.binning.operator.ui;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.operator.VariableConfig;

class VariableItem {

    VariableConfig variableConfig;

    VariableItem() {
        this.variableConfig = new VariableConfig();
    }

    VariableItem(VariableConfig variableConfig) {
        this.variableConfig = variableConfig;
    }
}
