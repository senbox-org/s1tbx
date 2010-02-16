package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class ByteConverterTest extends AbstractConverterTest {

    private ByteConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ByteConverter();
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Byte.class);

        testParseSuccess((byte) 124, "124");
        testParseSuccess((byte) -45, "-45");
        testParseSuccess((byte) 45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("124", (byte) 124);
        testFormatSuccess("-6", (byte) -6);
        testFormatSuccess("45", (byte) 45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
