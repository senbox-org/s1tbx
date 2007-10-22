package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class StringConverter implements com.bc.ceres.binding.Converter {
    public Class<?> getValueType() {
        return String.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text == null) {
            throw new NullPointerException("text");
        }
        return text;
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
