package com.bc.ceres.binding.descriptors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;

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
