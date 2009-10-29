package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.ValueAccessor;

/**
 * A value accessor for values stored as plain Java object.
 */
public class DefaultValueAccessor implements ValueAccessor {
    private Object value;

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     */
    public DefaultValueAccessor() {
    }

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     * @param value The initial value.
     */
    public DefaultValueAccessor(Object value) {
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
