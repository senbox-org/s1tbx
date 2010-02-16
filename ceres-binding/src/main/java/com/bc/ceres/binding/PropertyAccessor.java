package com.bc.ceres.binding;

/**
 * An {@code ValueAccessor} provides a strategy to get and set a value.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface PropertyAccessor {
    /**
     * The strategy to get a value.
     *
     * @return The value got.
     */
    Object getValue();

    /**
     * The strategy to set a value.
     *
     * @param value The value to be set.
     */
    void setValue(Object value);
}
