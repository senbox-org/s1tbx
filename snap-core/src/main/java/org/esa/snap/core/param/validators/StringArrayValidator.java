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
import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.ParamValidateException;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;

//@todo 1 se/** - add (more) class documentation

public class StringArrayValidator extends AbstractParamValidator {

    public StringArrayValidator() {
    }

    public Object parse(Parameter parameter, String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        if (isAllowedNullText(parameter, text)) {
            // return null, since null values are allowed
            return null;
        }

        final ParamProperties props = parameter.getProperties();
        final char[] separators = new char[]{props.getValueSetDelim()};
        return StringUtils.split(text, separators, true);
    }

    /**
     * Returns the given {@link String string array} value as a string, according to the rules of the {@link Parameter parameter}.
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
     *                              or the value is not an instance of {@link String String[]}.
     */
    public String format(Parameter parameter, Object value) throws ParamFormatException {

        if (isAllowedNullValue(parameter, value)) {
            return "";
        }

        String[] sa = castToStringArray(value);
        if (sa == null) {
            throw new ParamFormatException(parameter, ParamConstants.ERR_MSG_NOT_STRINGARRAY);
        }

        final ParamProperties props = parameter.getProperties();
        final String separator = new String(new char[]{props.getValueSetDelim()});
        return StringUtils.join((Object[]) value, separator);
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {

        validateThatNullValueIsAllowed(parameter, value);
        if (value == null) {
            // Null-test passed?
            return;
        }

        validateThatValueIsAStringArray(parameter, value);

        validateThatValueIsAnAllowedEmptyValue(parameter, value);

        validateThatValueIsBoundedInValueSet(parameter, value);

    }

    private void validateThatValueIsBoundedInValueSet(Parameter parameter, Object value) throws ParamValidateException {
        if (!parameter.getProperties().isValueSetBound()) {
            return;
        }

        String[] valueSet = (String[]) parameter.getProperties().getValueSet();
        if (valueSet == null) {
            return;
        }

        String[] sa = castToStringArray(value);
        for (int i = 0; i < sa.length; i++) {
            boolean found = false;
            for (int j = 0; j < valueSet.length; j++) {
                if (StringValidator.equalValues(parameter.getProperties().isCaseSensitive(), sa[i], valueSet[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_NOT_CONTAINED); /*I18N*/
            }
        }
    }

    private void validateThatValueIsAStringArray(Parameter parameter, Object value) throws ParamValidateException {
        String[] sa = castToStringArray(value);
        if (sa == null) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_NOT_STRINGARRAY_TYPE); /*I18N*/
        }
    }

    protected void validateThatValueIsAnAllowedEmptyValue(Parameter parameter, Object value) throws ParamValidateException {
        String[] maybeEmpty = castToStringArray(value);
        boolean emptyValuesNotAllowed = parameter.getProperties().isEmptyValuesNotAllowed();
        if (emptyValuesNotAllowed && maybeEmpty != null && maybeEmpty.length == 0) {
            throw new ParamValidateException(parameter, ParamConstants.ERR_MSG_EMPTY); /*I18N*/
        }
    }

    @Override
    public boolean equalValues(Parameter parameter, Object value1, Object value2) {

        String[] sa1 = castToStringArray(value1);
        String[] sa2 = castToStringArray(value2);

        if (sa1 == sa2) {
            return true;
        }

        if (sa1 == null && sa2 != null) {
            return false;
        }

        if (sa1 != null && sa2 == null) {
            return false;
        }

        if (sa1.length != sa2.length) {
            return false;
        }

        for (int i = 0; i < sa1.length; i++) {
            if (!StringValidator.equalValues(parameter.getProperties().isCaseSensitive(), sa1[i], sa2[i])) {
                return false;
            }
        }

        return true;
    }

    public int[] getValueSetIndices(Parameter parameter) {
        Debug.assertNotNull(parameter);
        String[] values = castToStringArray(parameter.getValue());
        String[] valueSet = castToStringArray(parameter.getProperties().getValueSet());
        if (values == null || valueSet == null) {
            return new int[0];
        }
        boolean caseSensitive = parameter.getProperties().isCaseSensitive();
        int numSelected = 0;
        for (int i = 0; i < values.length; i++) {
            int index = getValueSetIndex(valueSet, values[i], caseSensitive);
            if (index >= 0) {
                numSelected++;
            }
        }
        int[] indexes = new int[numSelected];
        for (int i = 0, j = 0; i < values.length; i++) {
            int index = getValueSetIndex(valueSet, values[i], caseSensitive);
            if (index >= 0) {
                indexes[j++] = index;
            }
        }
        return indexes;
    }

    protected static String[] castToStringArray(Object value) {
        return (value instanceof String[]) ? (String[]) value : null;
    }


    protected static int getValueSetIndex(String[] valueSet, String value, boolean caseSensitive) {
        for (int i = 0; i < valueSet.length; i++) {
            if (StringValidator.equalValues(caseSensitive, value, valueSet[i])) {
                return i;
            }
        }
        return -1;
    }
}
