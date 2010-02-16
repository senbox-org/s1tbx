package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class StringConverter implements com.bc.ceres.binding.Converter<String> {
    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    @Override
    public String parse(String text) throws ConversionException {
        if (text == null) {
            throw new NullPointerException("text");
        }
        return text;
    }

    @Override
    public String format(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
