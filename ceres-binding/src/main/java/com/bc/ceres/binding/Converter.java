package com.bc.ceres.binding;

/**
 * A {@code Converter} provides a strategy to convert a value of a certain type from
 * plain text to a Java object and vice versa.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface Converter<T> {
    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    Class<? extends T> getValueType();

    /**
     * Converts a value from its plain text representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param text The textual representation of the value.
     * @return The converted value.
     * @throws ConversionException If the conversion fails.
     */
    T parse(String text) throws ConversionException;

    /**
     * Converts a value of the type returned by {@link #getValueType()} to its
     * plain text representation.
     *
     * @param value The value to be converted to text.
     * @return The textual representation of the value.
     */
    String format(T value);
}
