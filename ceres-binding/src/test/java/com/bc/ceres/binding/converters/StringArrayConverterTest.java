package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class StringArrayConverterTest extends AbstractConverterTest {

    public StringArrayConverterTest() {
        super(new ArrayConverter(String[].class, new StringConverter()));
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(String[].class);
        testParseSuccess(new String[]{"a", "b", "c"}, "a,b,c");
        testParseSuccess(new String[]{"a", "b,", " c"}, " a, b%2C \t, +c");
        testFormatSuccess("a,b,c", new String[]{"a", "b", "c"});
        testFormatSuccess("a,b%2C,+c", new String[]{"a", "b,", " c"});
        assertNullCorrectlyHandled();
    }
}