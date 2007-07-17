package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

import java.io.File;

public class ObjectConverterTest extends AbstractConverterTest {

    public ObjectConverterTest() {
        super(new FileConverter());
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(File.class);

        File value = new File("/usr/local");

        testParseSuccess(value, "/usr/local");
        testParseSuccess(null, "");

        testFormatSuccess(value.toString(), value);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
