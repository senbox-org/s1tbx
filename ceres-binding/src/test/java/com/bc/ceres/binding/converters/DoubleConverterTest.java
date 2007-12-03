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

        testParseSuccess((double) 234, "234");
        testParseSuccess((double) -45.789, "-45.789");
        testParseSuccess((double) 0.25, "+0.25");
        testParseSuccess(null, "");

        testFormatSuccess("2353465.0", (double) 2353465);
        testFormatSuccess("-6.0", (double) -6);
        testFormatSuccess("0.0789", (double) 0.0789);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
