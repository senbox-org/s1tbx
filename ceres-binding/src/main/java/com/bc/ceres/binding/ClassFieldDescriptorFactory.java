package com.bc.ceres.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A factory for value descriptors derived from class {@link Field}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface ClassFieldDescriptorFactory {
    /**
     * Creates a new value descriptor for the given field.
     *
     * @param field The field.
     * @return The value descriptor.
     */
    ValueDescriptor createValueDescriptor(Field field);

    // todo  - think about extending and renaming this interface: PropertyDescriptorFactory
    // ValueDescriptor createValueDescriptor(Method getter, Method setter);

}
