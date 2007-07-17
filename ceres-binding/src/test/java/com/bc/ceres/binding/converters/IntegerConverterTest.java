package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class IntegerConverterTest extends AbstractConverterTest {

    public IntegerConverterTest() {
        super(new IntegerConverter());
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Integer.class);

        testParseSuccess((int)234, "234");
        testParseSuccess((int)-45, "-45");
        testParseSuccess((int)45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("2353465", (int)2353465);
        testFormatSuccess("-6", (int)-6);
        testFormatSuccess("45", (int)45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
