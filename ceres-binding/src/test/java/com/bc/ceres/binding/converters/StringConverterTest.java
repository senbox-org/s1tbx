package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class StringConverterTest extends AbstractConverterTest {

    public StringConverterTest() {
        super(new StringConverter());
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
