package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.awt.Font;

public class FontConverterTest extends AbstractConverterTest {

    private FontConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new FontConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Font.class);

        testParseSuccess(new Font("times", Font.PLAIN, 10), "times,plain,10");
        testParseSuccess(new Font("times", Font.BOLD, 11), "times, bold,11");
        testParseSuccess(new Font("times", Font.ITALIC, 12), "times, \titalic,12");
        testParseSuccess(new Font("times", Font.BOLD | Font.ITALIC, 14), "times,bolditalic,14");
        testParseSuccess(null, "");

        testFormatSuccess("times,plain,10", new Font("times", Font.PLAIN, 10));
        testFormatSuccess("times,bold,11", new Font("times", Font.BOLD, 11));
        testFormatSuccess("times,italic,12", new Font("times", Font.ITALIC, 12));
        testFormatSuccess("times,bolditalic,14", new Font("times", Font.BOLD | Font.ITALIC, 14));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
