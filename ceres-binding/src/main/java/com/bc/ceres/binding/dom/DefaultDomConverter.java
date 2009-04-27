package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;

import java.util.Collection;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter extends AbstractDomConverter {

    private final ClassFieldDescriptorFactory valueDescriptorFactory;

    public DefaultDomConverter(Class<?> valueType) {
        this(valueType, null);
    }

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
        final ValueContainer vc;
        if (valueDescriptorFactory != null) {
            vc = ValueContainer.createObjectBacked(value, valueDescriptorFactory);
        } else {
            vc = ValueContainer.createObjectBacked(value);
        }
        return vc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter createChildConverter(DomElement element, Class<?> valueType) {
        final String className = element.getAttribute("class");
        if (className != null && !className.trim().isEmpty()) {
            try {
                valueType = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (Map.class.isAssignableFrom(valueType)) {
            return new MapDomConverter(this);
        }
        if (Collection.class.isAssignableFrom(valueType)) {
            return new CollectionDomConverter(this);
        }
        return new DefaultDomConverter(valueType, valueDescriptorFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter getDomConverter(ValueDescriptor descriptor) {
        DomConverter domConverter = descriptor.getDomConverter();
        if (domConverter == null) {
            if (Map.class.isAssignableFrom(descriptor.getType())) {
                return new MapDomConverter(this);
            }
            if (Collection.class.isAssignableFrom(descriptor.getType())) {
                return new CollectionDomConverter(this);
            }
        }
        return domConverter;
    }

}