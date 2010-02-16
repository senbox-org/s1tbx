package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class ShortConverterTest extends AbstractConverterTest {

    private ShortConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ShortConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Short.class);

        testParseSuccess((short) 234, "234");
        testParseSuccess((short) -45, "-45");
        testParseSuccess((short) 45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("235", (short) 235);
        testFormatSuccess("-6", (short) -6);
        testFormatSuccess("45", (short) 45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
