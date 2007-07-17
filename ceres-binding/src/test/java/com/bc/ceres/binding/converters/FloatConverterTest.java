package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class FloatConverterTest extends AbstractConverterTest {

    public FloatConverterTest() {
        super(new FloatConverter());
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Float.class);

        testParseSuccess(234f, "234");
        testParseSuccess(-45.789f, "-45.789");
        testParseSuccess(0.25f, "+0.25");
        testParseSuccess(null, "");

        testFormatSuccess("2353465.0", 2353465f);
        testFormatSuccess("-6.0", -6f);
        testFormatSuccess("0.0789", 0.0789f);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
