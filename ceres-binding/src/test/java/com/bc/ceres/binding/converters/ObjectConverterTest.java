package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.io.File;

public class ObjectConverterTest extends AbstractConverterTest {

    private FileConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new FileConverter();
        }
        return converter;
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
