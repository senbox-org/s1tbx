package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class StringConverterTest extends AbstractConverterTest {

    private StringConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new StringConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(String.class);

        testParseSuccess("Ballamann!", "Ballamann!");
        testParseSuccess("", "");

        testFormatSuccess("Ballamann!", "Ballamann!");
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
