package com.bc.ceres.binding.converters;

public class LongConverter extends NumberConverter {
    @Override
    public Class<?> getValueType() {
        return Long.class;
    }

    @Override
    public Object parseNumber(String value) throws NumberFormatException {
        return Long.parseLong(value);
    }
}
