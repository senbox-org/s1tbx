package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.awt.Color;

public class ColorConverterTest extends AbstractConverterTest {

    private ColorConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ColorConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Color.class);

        testParseSuccess(new Color(17, 11, 67), "17,11,67");
        testParseSuccess(new Color(17, 11, 67, 127), "17, \t11, 67, 127");
        testParseSuccess(null, "");

        testFormatSuccess("17,11,67", new Color(17, 11, 67));
        testFormatSuccess("17,11,67,127", new Color(17, 11, 67, 127));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();

        try {
            converter.parse("17,11");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }

        try {
            converter.parse("17,11,67,1024");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }
    }
}
