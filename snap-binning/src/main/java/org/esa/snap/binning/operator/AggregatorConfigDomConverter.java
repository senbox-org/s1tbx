package org.esa.snap.binning.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;

/**
 * @author Norman Fomferra
 */
public class AggregatorConfigDomConverter extends TypedConfigDomConverter<AggregatorDescriptor, AggregatorConfig> {

    public AggregatorConfigDomConverter() {
        super(AggregatorDescriptor.class, AggregatorConfig.class);
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
            aggregatorConfigs[i] = (AggregatorConfig) super.convertDomToValue(child, null);
        }
        return aggregatorConfigs;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        AggregatorConfig[] aggregatorConfigs = (AggregatorConfig[]) value;
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            DomElement aggregator = parentElement.createChild("aggregator");
            super.convertValueToDom(aggregatorConfig, aggregator);
        }
    }
}
