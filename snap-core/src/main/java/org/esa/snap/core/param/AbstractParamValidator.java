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

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.SystemUtils;

import java.util.logging.Logger;

/**
 * The <code>AbstractParamValidator</code> acts as a base class for implementations of <code>ParamValidator</code>
 * interface by providing some default method implementations and several utility methods for common validators.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see ParamValidator
 */
public abstract class AbstractParamValidator implements ParamValidator {

    protected Logger _logger;

    protected AbstractParamValidator() {
        _logger = SystemUtils.LOG;
    }

    public boolean equalValues(Parameter parameter, Object value1, Object value2) {
        if (value1 == value2) {
            return true;
        }
        if (value1 == null) {
            return value2 == null;
        }
        return value1.equals(value2);
    }

    /**
     * Tests if the value passed in is null, and if so, if this is an allowed value defined by the parameter. When this
     * is not the case, the errorhandle passed in is invoked.
     *
     * @param parameter the Parameter defining the null value behaviour
     * @param value     the value to be checked
     */
    protected void validateThatNullValueIsAllowed(Parameter parameter, Object value) throws ParamValidateException {
        Debug.assertNotNull(parameter);
        if (value == null
            && !parameter.getProperties().isNullValueAllowed()) {
            throw new ParamValidateException(parameter, "Value must not be null."); /*I18N*/
        }
    }

    /**
     * Checks if a vector of value objects is contained in the valueset defined bay the parameter. If not, the error
     * handler passed in is invoked.
     *
     * @param parameter the Parameter defining the valueset
     * @param values    vector of objects to be checked
     */
    protected void validateThatValuesAreInValueSet(Parameter parameter, Object[] values) throws ParamValidateException {
        Debug.assertNotNull(parameter);
        Debug.assertNotNull(values);
        for (int i = 0; i < values.length; i++) {
            validateThatValueIsInValueSet(parameter, values[i]);
        }
    }

    /**
     * Checks if a value object is contained in the valueset defined by the parameter. If not, the error handler passed
     * in is invoked.
     *
     * @param parameter the Parameter defining the valueset
     * @param value     object to be checked
     */
    protected void validateThatValueIsInValueSet(Parameter parameter, Object value) throws ParamValidateException {
        Debug.assertNotNull(parameter);
        if (parameter.getProperties().getValueSet() != null
            && parameter.getProperties().isValueSetBound()
            && !isValueContainedInValueSet(parameter, value)) {
            throw new ParamValidateException(parameter, "Value is not allowed.");/*I18N*/
        }
    }

    /**
     * Tests if the value passed in is containe in the value set defined by the Parameter passed as argument.
     *
     * @param parameter the parameter whose value set is to be checked
     * @param value     the value to be checked
     */
    protected boolean isValueContainedInValueSet(Parameter parameter, Object value) {
        Debug.assertNotNull(parameter);
        String[] valueSet = parameter.getProperties().getValueSet();
        if (valueSet == null) {
            return false;
        }
        for (int i = 0; i < valueSet.length; i++) {
            String valueSetEntry = valueSet[i];
            // @todo 1 nf/nf - check: performance drawback, due to frequent parse() calls!
            Object value2 = null;
            try {
                value2 = parse(parameter, valueSetEntry);
                if (equalValues(parameter, value, value2)) {
                    return true;
                }
            } catch (ParamParseException e) {
                Debug.trace(ParamConstants.LOG_MSG_INVALID_VALUE_SET + parameter.getName() + "'");
                Debug.trace(e);
                _logger.fine(ParamConstants.LOG_MSG_INVALID_VALUE_SET + parameter.getName() + "'");
                _logger.fine(e.getMessage());
            }
        }
        return false;
    }


    /**
     * Tests if the given text is an allowed zero-length text string for the given parameter.
     *
     * @param parameter the parameter
     * @param text      the text to test
     *
     * @return <code>true</code> if so
     */
    protected static boolean isAllowedNullText(Parameter parameter, String text) {
        Debug.assertNotNull(parameter);
        Debug.assertNotNull(text);
        return text.trim().length() == 0
               && parameter.getProperties().isNullValueAllowed();
    }

    /**
     * Tests if the given object is an allowed null value for the given parameter.
     *
     * @param parameter the parameter
     * @param value     the value to test
     *
     * @return <code>true</code> if so
     */
    protected static boolean isAllowedNullValue(Parameter parameter, Object value) {
        Debug.assertNotNull(parameter);
        return value == null
               && parameter.getProperties().isNullValueAllowed();
    }

}
