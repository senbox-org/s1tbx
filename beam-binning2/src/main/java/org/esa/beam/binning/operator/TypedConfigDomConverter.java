package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.beam.binning.TypedConfig;
import org.esa.beam.binning.TypedDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;

/**
 * @author Norman Fomferra
 * @author Marco Zuehlke
 *
 * @see TypedConfig
 * @see TypedDescriptor
 */
public class TypedConfigDomConverter<TD extends TypedDescriptor, TC extends TypedConfig> implements DomConverter {

    private final Class<TD> descriptorClass;
    private final Class<TC> configClass;
    private DefaultDomConverter childConverter;

    protected TypedConfigDomConverter(Class<TD> descriptorClass, Class<TC> configClass) {
        this.descriptorClass = descriptorClass;
        this.configClass = configClass;
        this.childConverter  = new DefaultDomConverter(configClass);
    }

    @Override
    public Class<?> getValueType() {
        return configClass;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        DomElement typeElement = parentElement.getChild("type");
        String typeName = typeElement.getValue();
        TC config;
        if (value == null) {
            config = createConfig(typeName);
        } else {
            config = (TC) value;
        }
        childConverter.convertDomToValue(parentElement, config);
        return config;
    }

    protected TC createConfig(String name) {
        Assert.notNull(name, "name");
        TypedDescriptor<TC> descriptor = TypedDescriptorsRegistry.getInstance().getDescriptor(descriptorClass, name);
        Assert.argument(descriptor != null, String.format("Unknown name '%s'", name));
        return descriptor.createConfig();
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        childConverter.convertValueToDom(value, parentElement);
    }
}
