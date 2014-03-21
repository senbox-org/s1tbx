package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * Default implementation for the {@link PropertyDescriptorFactory} interface.
 *
 * @author Norman Fomferra
 * @since 0.14
 */
public class DefaultPropertyDescriptorFactory implements PropertyDescriptorFactory {

    /**
     * Creates a new property descriptor using the field's name and type.
     * @param field The field.
     * @return The new property descriptor. Never {@code null}.
     */
    @Override
    public PropertyDescriptor createValueDescriptor(Field field) {
        boolean isDeprecated = field.getAnnotation(Deprecated.class) != null;
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), field.getType());
        propertyDescriptor.setDeprecated(isDeprecated);
        return propertyDescriptor;
    }
}
