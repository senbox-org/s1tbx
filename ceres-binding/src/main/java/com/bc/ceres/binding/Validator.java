package com.bc.ceres.binding;

/**
 * A {@code Validator} provides the strategy to validate a value with respect to a provided
 * {@link ValueModel}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface Validator {
    /**
     * Validates a value with respect to a provided value model.
     *
     * @param valueModel The value model.
     * @param value      The value to be validated.
     * @throws ValidationException If validation fails.
     */
    void validateValue(ValueModel valueModel, Object value) throws ValidationException;
}
