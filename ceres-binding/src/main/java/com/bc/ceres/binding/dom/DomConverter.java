package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;

/**
 * A {@code DomConverter} provides a strategy to convert a value of a certain type from
 * an (XML) DOM to a Java value and vice versa. Its common use is to convert complex value types
 * from and to XML.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 0.6
 */
public interface DomConverter<T> {
    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    Class<T> getValueType();

    /**
     * Converts a DOM to a (complex) value of the type returned by {@link #getValueType()}.
     *
     * @param parentElement The parent DOM element.
     * @param value  The value which receives the DOM representation of the value or {@code null}.
     * If {@code value} is {@code null}, the method is responsible for the creation of a new instance
     * and its configuration using the DOM. @return The converted and configured value.
     * @throws com.bc.ceres.binding.ConversionException If the conversion fails.
     * @throws com.bc.ceres.binding.ValidationException If the converted value is invalid.
     * @return The converted value, never {@code null}.
     */
    T convertDomToValue(DomElement parentElement, T value) throws ConversionException, ValidationException;

    /**
     * Converts a (complex) value of the type returned by {@link #getValueType()} to its
     * DOM representation which may be further converted to XML. The given {@code value} can be
     * safely cast to the type returned by {@link #getValueType()}.
     *
     * @param value The value to be converted to the DOM.
     * @param parentElement The parent DOM element, which receives the DOM representation of the value.
     */
    void convertValueToDom(T value, DomElement parentElement);
}
