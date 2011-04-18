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

package org.esa.beam.visat.dialogs;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.AbstractParamValidator;
import org.esa.beam.framework.param.ParamFormatException;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;

import java.text.ParseException;
import java.util.Date;

public class DateValidator extends AbstractParamValidator {

    @Override
    public Object parse(Parameter parameter, String text) throws ParamParseException {
        try {
            return ProductData.UTC.parse(text).getAsDate();
        } catch (ParseException ignore) {
            final String msg = String.format("Date format must be '%s'", ProductData.UTC.DATE_FORMAT_PATTERN);
            throw new ParamParseException(parameter, msg);
        }
    }

    @Override
    public String format(Parameter parameter, Object value) throws ParamFormatException {
        final ProductData.UTC utc = ProductData.UTC.create((Date) value, 0);
        return utc.format();
    }

    @Override
    public void validate(Parameter parameter, Object value) throws ParamValidateException {
        validateThatNullValueIsAllowed(parameter, value);
        if (value == null) {
            return;
        }

        if (!(value instanceof Date)) {
            throw new ParamValidateException(parameter, "Value must be an instance of Date");
        }
    }

}
