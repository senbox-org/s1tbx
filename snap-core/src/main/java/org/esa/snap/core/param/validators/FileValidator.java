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

import java.io.File;

//@todo 1 se/** - add (more) class documentation

public class FileValidator extends AbstractParamValidator {

    public FileValidator() {
    }

    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        String trimedText = text.trim();
        if (isAllowedNullText(parameter, trimedText)) {
            return null;
        }

        return new File(trimedText);
    }

    /**
     * Returns the given {@link File file} value as a string, according to the rules of the {@link Parameter parameter}.
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
     *                              or the value is not an instance of {@link File}.
     */
    public String format(Parameter parameter, Object value) throws ParamFormatException {

        if (isAllowedNullValue(parameter, value)) {
            return "";
        }

        File fValue = castToFile(value);
        if (fValue == null) {
            throw new ParamFormatException(parameter, ParamConstants.ERR_MSG_NOT_FILE);
        }

        return fValue.getPath();
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {

        validateThatNullValueIsAllowed(parameter, value);

        File fValue = castToFile(value);
        if (fValue == null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_MUST_BE_FILE); /*I18N*/
        }

        /*
         * @todo 1 nf/nf - add more test here: parameter.getProperties() can
         * contain specific file attributes ("dirOnly", "mustExists" ...)
         */

        validateThatValueIsInValueSet(parameter, value);
    }

    @Override
    public boolean equalValues(Parameter parameter, Object value1, Object value2) {

        File f1 = castToFile(value1);
        File f2 = castToFile(value2);

        if (f1 == f2) {
            return true;
        }

        if (f1 == null && f2 != null) {
            return false;
        }

        if (f1 != null && f2 == null) {
            return false;
        }

        return f1.compareTo(f2) == 0;
    }


    protected static File castToFile(Object value) {
        return (value instanceof File) ? (File) value : null;
    }
}
