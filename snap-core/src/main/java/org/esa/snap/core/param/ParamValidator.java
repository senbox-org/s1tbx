/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.param;

/**
 * A <code>ParamValidator</code> is an interface for parameter type specific value-to-text and text-to-value
 * conversions, data validation and comparision.
 * <p> This interface strictly defines algorithms, implementors of this interface should refrain from defining any data
 * in their implementing classes. Any information required to correctly perform the operations defined in
 * <code>ParamValidator</code> shall be supplied by the given <code>Parameter</code>.
 * <p> Parameter validators for a given Java class can be permanently registered in <code>ParamValidator</code>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public interface ParamValidator {

    /**
     * Converts the given text into a value taking the given parameter information into account.
     *
     * @param parameter the parameter, must not be <code>null</code>
     * @param text      the text to be converted into a value, must not be <code>null</code>
     *
     * @return the value represented by the text
     */
    Object parse(Parameter parameter, String text) throws ParamParseException;

    /**
     * Converts the given value into a text taking the given parameter information into account. 
     *
     * @param parameter the parameter, must not be <code>null</code>
     * @param value     the value to be converted into a text, can be <code>null</code>
     *
     * @return the value represented by the text, never <code>null</code>.
     */
    String format(Parameter parameter, Object value) throws ParamFormatException;

    /**
     * Tests if the given value passes all constraints given in the supplied parameter information. The value can also
     * be <code>null</code> since parameters can be allowed to have the value 'null'.
     *
     * @param parameter the parameter, must not be <code>null</code>
     * @param value     the value to be tested, can be <code>null</code>
     */
    void validate(Parameter parameter, Object value) throws ParamValidateException;

    /**
     * Tests if the given two values are equal taking the given parameter information into account.
     *
     * @param parameter the parameter, must not be <code>null</code>
     * @param value1    the first value, can be <code>null</code>
     * @param value2    the second value, can also be <code>null</code>
     *
     * @return <code>true</code> if the value are equal, <code>false</code> otherwise
     */
    boolean equalValues(Parameter parameter, Object value1, Object value2);
}
