package com.bc.ceres.binding.converters;

public class ShortConverter extends NumberConverter<Short> {
    @Override
    public Class<Short> getValueType() {
        return Short.class;
    }

    @Override
    public Short parseNumber(String text) throws NumberFormatException {
        return Short.parseShort(text);
    }
}
