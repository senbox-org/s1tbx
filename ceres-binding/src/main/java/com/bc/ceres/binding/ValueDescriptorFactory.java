package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * A factory for value descriptors derived from object {@link Field}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface ValueDescriptorFactory {
    /**
     * Creates a new value descriptor for the given field.
     *
     * @param field The field.
     * @return The value descriptor.
     */
    ValueDescriptor createValueDescriptor(Field field);
}
