package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternConverter implements com.bc.ceres.binding.Converter {

    public Class<?> getValueType() {
        return Pattern.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(text);
        } catch (PatternSyntaxException e) {
            throw new ConversionException(e.getMessage(), e);
        }
    }

    public String format(Object value) {
        return value.toString();
    }
}
