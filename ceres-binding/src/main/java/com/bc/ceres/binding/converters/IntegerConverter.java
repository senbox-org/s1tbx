package com.bc.ceres.binding.converters;


public class IntegerConverter extends NumberConverter {
    @Override
    public Class<?> getValueType() {
        return Integer.class;
    }

    @Override
    public Object parseNumber(String value) throws NumberFormatException {
        return Integer.parseInt(value);
    }
}
