package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class BooleanConverter implements Converter<Boolean> {
    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    @Override
    public String format(Boolean value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
