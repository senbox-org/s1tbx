package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.io.File;

public class FileConverter implements Converter {

    public Class<?> getValueType() {
        return File.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return new File(text);
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return ((File) value).getPath();
    }
}
