package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * A factory for property accessors derived from Java class fields.
 * @since Ceres 0.10
 */
public interface PropertyAccessorFactory {
    /**
     * Creates a new property accessor instance.
     *
     * @param field A Java class field.
     * @return A new property accessor, or {@code null}.
     */
    PropertyAccessor createValueAccessor(Field field);
}
