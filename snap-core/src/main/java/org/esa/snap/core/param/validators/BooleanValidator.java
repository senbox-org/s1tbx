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

//@todo 1 se/** - add (more) class documentation

public class BooleanValidator extends AbstractParamValidator {

    public static final String TRUE_STRING = "true";
    public static final String FALSE_STRING = "false";

    public BooleanValidator() {
    }

    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        String trimedText = text.trim();
        if (isAllowedNullText(parameter, trimedText)) {
            return null;
        }

        if (trimedText.equalsIgnoreCase(TRUE_STRING)) {
            return Boolean.TRUE;
        } else if (trimedText.equalsIgnoreCase(FALSE_STRING)) {
            return Boolean.FALSE;
        } else {
            throw new ParamParseException(parameter, ParamConstants.ERR_MSG_INVALID_BOOLEAN); /*I18N*/
        }
    }

    /**
     * Returns the given {@link Boolean} value as a string, according to the rules of the given {@link Parameter parameter}.
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
     *                              or the value is not an instance of {@link Boolean}.
     */
    public String format(Parameter parameter, Object value) throws ParamFormatException {

        if (isAllowedNullValue(parameter, value)) {
            return "";
        }

        Boolean bValue = castToBoolean(value);
        if (bValue == null) {
            throw new ParamFormatException(parameter, ParamConstants.ERR_MSG_NOT_BOOLEAN);
        }

        return bValue ? TRUE_STRING : FALSE_STRING;
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {

        validateThatNullValueIsAllowed(parameter, value);

        Boolean b = castToBoolean(value);
        if (b == null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_NOT_BOOLEAN_TYPE); /*I18N*/
        }
    }

    protected static Boolean castToBoolean(Object value) {
        return (value instanceof Boolean) ? (Boolean) value : null;
    }
}
