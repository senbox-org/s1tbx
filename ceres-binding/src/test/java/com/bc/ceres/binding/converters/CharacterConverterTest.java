package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class CharacterConverterTest extends AbstractConverterTest {

    public CharacterConverterTest() {
        super(new CharacterConverter());
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Character.class);

        testParseSuccess('5', "5");
        testParseSuccess('B', "B");
        testParseSuccess(null, "");

        testParseFailed("Ballamann!");

        testFormatSuccess("B", 'B');
        testFormatSuccess("5", '5');
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }

}
