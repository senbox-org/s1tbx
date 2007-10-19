package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

public class CharacterConverter implements Converter {
    public Class<?> getValueType() {
        return Character.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > 1) {
            throw new ConversionException("Not a character: " + text);
        }
        return text.charAt(0);
    }

    public String format(Object value)  {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
