package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * A factory for value descriptors derived from Java class {@link Field}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface PropertyDescriptorFactory {
    /**
     * Creates a new value descriptor for the given field.
     *
     * @param field The field.
     * @return The value descriptor.
     */
    PropertyDescriptor createValueDescriptor(Field field);
}
