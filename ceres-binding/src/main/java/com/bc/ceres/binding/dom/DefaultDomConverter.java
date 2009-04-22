package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter extends AbstractDomConverter {

    private final ClassFieldDescriptorFactory valueDescriptorFactory;

    public DefaultDomConverter(Class<?> valueType, ClassFieldDescriptorFactory valueDescriptorFactory) {
        super(valueType);
        this.valueDescriptorFactory = valueDescriptorFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueContainer getValueContainer(Object value) {
        if (value instanceof ValueContainer) {
            return (ValueContainer) value;
        }
        return ValueContainer.createObjectBacked(value, valueDescriptorFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter createChildConverter(DomElement element, Class<?> valueType) {
        return new DefaultDomConverter(valueType, valueDescriptorFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter getDomConverter(ValueDescriptor descriptor) {
        return descriptor.getDomConverter();
    }
}