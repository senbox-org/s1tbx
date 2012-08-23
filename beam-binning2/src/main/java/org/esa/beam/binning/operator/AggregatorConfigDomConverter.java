package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;

/**
 * @author Norman Fomferra
 */
public class AggregatorConfigDomConverter implements DomConverter {

    private DefaultDomConverter childConverter;

    public AggregatorConfigDomConverter() {
        this.childConverter  = new DefaultDomConverter(AggregatorConfig.class);
    }

    @Override
    public Class<?> getValueType() {
        return AggregatorConfig[].class;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        int childCount = parentElement.getChildCount();
        AggregatorConfig[] aggregatorConfigs = new AggregatorConfig[childCount];
        for (int i = 0; i < childCount; i++) {
            DomElement child = parentElement.getChild(i);
            String aggregatorName = child.getChild("type").getValue();
            AggregatorConfig aggregatorConfig = createAggregatorConfig(aggregatorName);
            childConverter.convertDomToValue(child, aggregatorConfig);
            aggregatorConfigs[i] = aggregatorConfig;
        }
        return aggregatorConfigs;
    }

    private AggregatorConfig createAggregatorConfig(String aggregatorName) {
        // todo - use this instead (nf, LC-aggregation)
        // final AggregatorDescriptor aggregatorDescriptor = AggregatorDescriptorRegistry.getInstance().getAggregatorDescriptor(aggregatorName);
        // return aggregatorDescriptor.createAggregatorConfig();
        return new AggregatorConfig(aggregatorName);
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        DomElement aggregators = parentElement.createChild("aggregators");
        AggregatorConfig[] aggregatorConfigs = (AggregatorConfig[]) value;
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            DomElement aggregator = aggregators.createChild("aggregator");
            childConverter.convertValueToDom(aggregatorConfig, aggregator);
        }
        parentElement.addChild(aggregators);

    }
}
