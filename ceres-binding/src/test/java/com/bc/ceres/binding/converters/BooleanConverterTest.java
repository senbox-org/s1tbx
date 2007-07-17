package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class BooleanConverterTest extends AbstractConverterTest {

    public BooleanConverterTest() {
        super(new BooleanConverter());
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
