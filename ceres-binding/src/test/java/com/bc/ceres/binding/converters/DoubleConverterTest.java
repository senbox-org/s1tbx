package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class DoubleConverterTest extends AbstractConverterTest {

    private DoubleConverter converter;


    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new DoubleConverter();
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Double.class);

        testParseSuccess(234.0, "234");
        testParseSuccess(-45.789, "-45.789");
        testParseSuccess(0.25, "+0.25");
        testParseSuccess(null, "");
        testParseSuccess(Double.NaN, "NaN");

        testFormatSuccess("2353465.0", 2353465.0);
        testFormatSuccess("-6.0", -6.0);
        testFormatSuccess("0.0789", 0.0789);
        testFormatSuccess("", null);
        testFormatSuccess("NaN", Double.NaN);

        assertNullCorrectlyHandled();
    }
}
