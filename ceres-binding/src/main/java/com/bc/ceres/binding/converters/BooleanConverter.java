package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class BooleanConverter implements Converter {
    public Class<?> getValueType() {
        return Boolean.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
