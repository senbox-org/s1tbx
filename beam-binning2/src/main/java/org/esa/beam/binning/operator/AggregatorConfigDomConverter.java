package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.AggregatorDescriptorRegistry;

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
            DomElement typeElement = child.getChild("type");
            String aggregatorName = typeElement.getValue();
            AggregatorConfig aggregatorConfig = createAggregatorConfig(aggregatorName);
            childConverter.convertDomToValue(child, aggregatorConfig);
            aggregatorConfigs[i] = aggregatorConfig;
        }
        return aggregatorConfigs;
    }

    private AggregatorConfig createAggregatorConfig(String aggregatorName) {
        Assert.notNull(aggregatorName, "aggregatorName");
        final AggregatorDescriptor aggregatorDescriptor = AggregatorDescriptorRegistry.getInstance().getAggregatorDescriptor(aggregatorName);
        Assert.argument(aggregatorDescriptor != null, String.format("Unknown aggregator name '%s'", aggregatorName));
        return aggregatorDescriptor.createAggregatorConfig();
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        AggregatorConfig[] aggregatorConfigs = (AggregatorConfig[]) value;
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            DomElement aggregator = parentElement.createChild("aggregator");
            childConverter.convertValueToDom(aggregatorConfig, aggregator);
        }

    }
}
