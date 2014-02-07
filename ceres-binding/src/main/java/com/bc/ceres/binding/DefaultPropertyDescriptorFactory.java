package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * @author Norman Fomferra
 * @since 0.14
*/
public class DefaultPropertyDescriptorFactory implements PropertyDescriptorFactory {

    @Override
    public PropertyDescriptor createValueDescriptor(Field field) {
        return new PropertyDescriptor(field.getName(), field.getType());
    }
}
