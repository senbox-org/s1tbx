package com.bc.ceres.binding;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

/**
 * A {@code XmlConverter} provides a strategy to convert a value of a certain type from
 * an XML DOM to a Java object and vice versa. Its common use is to convert complex value types
 * from and to XML.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 0.6
 */
public interface XmlConverter {
    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    Class<?> getValueType();

    /**
     * Converts a (complex) value from its XML DOM representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param dom The parent XML DOM , which either contains the representation of the value.
     * @return The value converted to the XML DOM.
     * @throws ConversionException If the conversion fails.
     */
    Object convertDomToValue(Xpp3Dom dom) throws ConversionException;

    /**
     * Converts a (complex) value of the type returned by {@link #getValueType()} to its
     * XML DOM representation. The given {@code value} can be safely cast to the type 
     * returned by {@link #getValueType()}.
     *
     * @param value The value to be converted to the XML DOM.
     * @param dom The parent XML DOM , which either <i>contains</i> or <i>is</i> the representation of the value.
     */
    void convertValueToDom(Object value, Xpp3Dom dom);
}
