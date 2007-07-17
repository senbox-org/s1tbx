package com.bc.ceres.binding.converters;

public class ShortConverter extends NumberConverter {
    @Override
    public Class<?> getValueType() {
        return Short.class;
    }

    @Override
    public Object parseNumber(String text) throws NumberFormatException {
        return Short.parseShort(text);
    }
}
