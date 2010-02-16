package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class BooleanConverterTest extends AbstractConverterTest {

    private BooleanConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new BooleanConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Boolean.class);

        testParseSuccess(Boolean.TRUE, "true");
        testParseSuccess(Boolean.FALSE, "false");
        testParseSuccess(Boolean.FALSE, "Raps!");
        testParseSuccess(null, "");

        testFormatSuccess("true", Boolean.TRUE);
        testFormatSuccess("false", Boolean.FALSE);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
