package com.bc.ceres.binding;

/**
 * A {@code Validator} provides the strategy to validate a value with respect to a provided
 * {@link Property}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface Validator {
    /**
     * Validates a value with respect to a provided property.
     *
     * @param property The property.
     * @param value      The value to be validated.
     * @throws ValidationException If validation fails.
     */
    void validateValue(Property property, Object value) throws ValidationException;
}
