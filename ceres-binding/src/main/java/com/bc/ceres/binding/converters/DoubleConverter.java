package com.bc.ceres.binding.converters;


public class DoubleConverter extends NumberConverter {

    @Override
    public Class<?> getValueType() {
        return Double.class;
    }

    @Override
    public Object parseNumber(String value) throws NumberFormatException {
        return Double.parseDouble(value);
    }
}
