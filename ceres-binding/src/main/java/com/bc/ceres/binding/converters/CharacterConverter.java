package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

public class CharacterConverter implements Converter<Character> {
    @Override
    public Class<Character> getValueType() {
        return Character.class;
    }

    @Override
    public Character parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > 1) {
            throw new ConversionException("Not a character: " + text);
        }
        return text.charAt(0);
    }

    @Override
    public String format(Character value)  {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
