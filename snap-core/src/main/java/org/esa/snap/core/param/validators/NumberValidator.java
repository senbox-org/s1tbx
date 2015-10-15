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

public class NumberValidator extends AbstractParamValidator {

    public NumberValidator() {
    }

    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        String trimmedText = text.trim();
        if (isAllowedNullText(parameter, trimmedText)) {
            return null;
        }

        Number number = null;
        try {
            if (parameter.getValueType() == Double.class) {
                number = Double.valueOf(trimmedText);
            } else if (parameter.getValueType() == Float.class) {
                number = Float.valueOf(trimmedText);
            } else if (parameter.getValueType() == Long.class) {
                number = Long.valueOf(trimmedText);
            } else if (parameter.getValueType() == Integer.class) {
                number = Integer.valueOf(trimmedText);
            } else if (parameter.getValueType() == Short.class) {
                number = Short.valueOf(trimmedText);
            } else if (parameter.getValueType() == Byte.class) {
                number = Byte.valueOf(trimmedText);
            } else {
                throw new ParamParseException(parameter, ParamConstants.ERR_MSG_UNSUPP_FORMAT);
            }
        } catch (NumberFormatException e) {
            final Object[] msgArgs = new Object[]{parameter.getValueType().getSimpleName().toLowerCase()};
            final String message = String.format(ParamConstants.ERR_MSG_MUST_BE_NUMBER, msgArgs);
            throw new ParamParseException(parameter, message); /*I18N*/
        }

        return number;
    }

    /**
     * Returns the given {@link Number number} value as a string, according to the rules of the {@link Parameter parameter}.
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
     *                              or the value is not an instance of {@link Number}.
     */
    public String format(Parameter parameter, Object value) throws ParamFormatException {

        if (isAllowedNullValue(parameter, value)) {
            return "";
        }

        Number n = castToNumber(value);
        if (n == null) {
            throw new ParamFormatException(parameter, ParamConstants.ERR_MSG_NOT_NUMBER_TYPE);
        }

        return n.toString();
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {

        validateThatNullValueIsAllowed(parameter, value);

        Number n = castToNumber(value);
        if (n == null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_MUST_BE_NUMBER); /*I18N*/
        }

        Number min = parameter.getProperties().getMinValue();
        if (min != null && n.doubleValue() < min.doubleValue()) {
            handleOutOfRangeError(parameter);
        }

        Number max = parameter.getProperties().getMaxValue();
        if (max != null && n.doubleValue() > max.doubleValue()) {
            handleOutOfRangeError(parameter);
        }

/*
         @todo 1 nf/nf - check this out
        Number inc = parameter.getProperties().getIncrement();
        if (inc != null && Math.IEEEremainder(n.doubleValue(), inc.doubleValue()) != 0.0) {
            Debug.trace("value must devidable by " + inc);
        }
*/

        validateThatValueIsInValueSet(parameter, value);
    }

    /**
     * Compares two parameter values for equality. It is assumed that both objects passed in as parametrer are numbers.
     *
     * @param parameter not used in this context
     * @param value1    first value to be compared
     * @param value2    second value to be compared
     */
    @Override
    public boolean equalValues(Parameter parameter, Object value1, Object value2) {

        Number n1 = castToNumber(value1);
        Number n2 = castToNumber(value2);

        if (n1 == n2) {
            return true;
        }

        if (n1 == null && n2 != null) {
            return false;
        }

        if (n1 != null && n2 == null) {
            return false;
        }

        return n1.equals(n2);
    }

    protected void handleOutOfRangeError(Parameter parameter) throws ParamValidateException {
        Number min = parameter.getProperties().getMinValue();
        Number max = parameter.getProperties().getMaxValue();

        if (min != null && max != null) {
            throw new ParamValidateException(parameter,
                                             ParamConstants.ERR_MSG_VALUE_IN_RANGE + min + " to " + max + "."); /*I18N*/
        } else if (min != null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_VALUE_GREATER + min + "."); /*I18N*/
        } else if (max != null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_VALUE_LESS + max + "."); /*I18N*/
        }
    }

    protected static Number castToNumber(Object value) {
        return (value instanceof Number) ? (Number) value : null;
    }
}
