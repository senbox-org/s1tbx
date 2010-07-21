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

package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatConverter implements Converter<Date>{
    private DateFormat format;

    public DateFormatConverter() {
        this(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public DateFormatConverter(DateFormat format) {
        this.format = format;
    }

    @Override
    public Class<Date> getValueType() {
        return Date.class;
    }

    @Override
    public Date parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return format.parse(text);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(Date value) {
        if (value == null) {
            return "";
        }
        return format.format( value);
    }
}
