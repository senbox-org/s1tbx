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
package org.esa.snap.core.param.validators;

import org.esa.snap.core.param.AbstractParamValidator;
import org.esa.snap.core.param.ParamConstants;
import org.esa.snap.core.param.ParamFormatException;
import org.esa.snap.core.param.ParamParseException;
import org.esa.snap.core.param.ParamValidateException;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;

//@todo 1 se/** - add (more) class documentation

public class StringValidator extends AbstractParamValidator {

    public StringValidator() {
    }

    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        if (isAllowedNullText(parameter, text)) {
            return null;
        }

        return text;
    }

    /**
     * Returns the given {@link String string} value as a string, according to the rules of the {@link Parameter parameter}.
     * If the value is <code>null</code> and {@link #isAllowedNullValue null value is allowed},
     * this method returns an empty string, otherwise a {@link ParamFormatException} will be thrown.
     *
     * @param parameter the parameter which contains the rules to format
     * @param value     the value to format
     *
     * @return the value as string or an empty string.
     *
     * @throws ParamFormatException if the value is <code>null</code> and
     *                              {@link #isAllowedNullValue null value is not allowed}
     *                              or the value is not an instance of {@link String}.
     */
    public String format(Parameter parameter, Object value) throws ParamFormatException {

        if (isAllowedNullValue(parameter, value)) {
            return "";
        }

        String s = castToString(value);
        if (s == null) {
            throw new ParamFormatException(parameter, ParamConstants.ERR_MSG_NOT_A_STRING); /*I18N*/
        }

        return s;
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {

        validateThatNullValueIsAllowed(parameter, value);
        if (value == null) {
            return;
        }

        validateThatValueIsAString(parameter, value);

        validateThatValueIsAnIdentifier(parameter, value);

        validateThatValueIsAnAllowedEmptyValue(parameter, value);

        validateThatValueIsInValueSet(parameter, value);
    }

    protected void validateThatValueIsAString(Parameter parameter, Object value) throws ParamValidateException {
        String s = castToString(value);
        if (s == null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_MUST_BE_STRING); /*I18N*/
        }
    }

    protected void validateThatValueIsAnIdentifier(Parameter parameter, Object value) throws ParamValidateException {
        String name = (String) value;
        boolean identifiersOnly = parameter.getProperties().isIdentifiersOnly();
        if (identifiersOnly && !StringUtils.isIdentifier(name)) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_NO_IDENTIFIER); /*I18N*/
        }
    }

    protected void validateThatValueIsAnAllowedEmptyValue(Parameter parameter, Object value) throws ParamValidateException {
        String maybeEmpty = (String) value;
        boolean emptyValuesNotAllowed = parameter.getProperties().isEmptyValuesNotAllowed();
        if (emptyValuesNotAllowed && maybeEmpty != null && maybeEmpty.trim().length() == 0) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_EMPTY); /*I18N*/
        }
    }

    @Override
    public boolean equalValues(Parameter parameter, Object value1, Object value2) {
        return equalValues(parameter.getProperties().isCaseSensitive(), value1, value2);
    }

    public static boolean equalValues(boolean caseSensitive, Object value1, Object value2) {

        String s1 = castToString(value1);
        String s2 = castToString(value2);

        if (s1 == s2) {
            return true;
        }

        if (s1 == null && s2 != null) {
            return false;
        }

        if (s1 != null && s2 == null) {
            return false;
        }

        return caseSensitive ? s1.equals(s2) : s1.equalsIgnoreCase(s2);
    }

    protected static String castToString(Object value) {
        return (value instanceof String) ? (String) value : null;
    }
}
