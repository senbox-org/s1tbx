package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternConverter implements Converter<Pattern> {

    @Override
    public Class<Pattern> getValueType() {
        return Pattern.class;
    }

    @Override
    public Pattern parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(text);
        } catch (PatternSyntaxException e) {
            throw new ConversionException(e.getMessage(), e);
        }
    }

    @Override
    public String format(Pattern value) {
        return value.toString();
    }
}
