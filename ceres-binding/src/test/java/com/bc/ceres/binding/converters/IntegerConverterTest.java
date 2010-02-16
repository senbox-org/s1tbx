package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class IntegerConverterTest extends AbstractConverterTest {

    private IntegerConverter converter;


    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new IntegerConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Integer.class);

        testParseSuccess(234, "234");
        testParseSuccess(-45, "-45");
        testParseSuccess(45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("2353465", 2353465);
        testFormatSuccess("-6", -6);
        testFormatSuccess("45", 45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
