package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class CharacterConverterTest extends AbstractConverterTest {

    private CharacterConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new CharacterConverter();
        }
        return converter;
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
