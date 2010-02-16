package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.PropertyAccessor;

/**
 * A value accessor for values stored as plain Java object.
 */
public class DefaultPropertyAccessor implements PropertyAccessor {
    private Object value;

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     */
    public DefaultPropertyAccessor() {
    }

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     * @param value The initial value.
     */
    public DefaultPropertyAccessor(Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object value) {
        this.value = value;
    }
}
