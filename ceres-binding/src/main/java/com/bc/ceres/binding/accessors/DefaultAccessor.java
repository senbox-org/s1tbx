package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.Accessor;

/**
 * Accessor for values stored as plain Java object.
 */
public class DefaultAccessor implements Accessor {
    private Object value;

    /**
     * Constructs a new {@code DefaultAccessor} with a {@code null} default value.
     */
    public DefaultAccessor() {
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
