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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatConverterTest extends AbstractConverterTest {

    private DateFormatConverter converter;
    private TimeZone oldTimeZone;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new DateFormatConverter();
        }
        return converter;
    }

    @Override
    protected void setUp() throws Exception {
        oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Override
    protected void tearDown() throws Exception {
        TimeZone.setDefault(oldTimeZone);
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Date.class);

        Date someDate = null;
        try {
            someDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2007-05-27 16:10:15");
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        testParseSuccess(new Date(0L), "1970-01-01 00:00:00");
        testParseSuccess(someDate, "2007-05-27 16:10:15");
        testParseSuccess(null, "");

        testFormatSuccess("1970-01-01 00:00:00", new Date(0L));
        testFormatSuccess("2007-05-27 16:10:15", someDate);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
