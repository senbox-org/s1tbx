package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class DateFormatConverterTest extends AbstractConverterTest {

    public DateFormatConverterTest() {
        super(new DateFormatConverter());
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

        testParseSuccess(new Date(0L), "1970-01-01 01:00:00");
        testParseSuccess(someDate, "2007-05-27 16:10:15");
        testParseSuccess(null, "");

        testFormatSuccess("1970-01-01 01:00:00", new Date(0L));
        testFormatSuccess("2007-05-27 16:10:15", someDate);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
