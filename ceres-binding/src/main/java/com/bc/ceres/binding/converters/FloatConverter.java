package com.bc.ceres.binding.converters;

public class FloatConverter extends NumberConverter {

    @Override
    public Class<?> getValueType() {
        return Float.class;
    }

    @Override
    public Object parseNumber(String value) throws NumberFormatException {
        return Float.parseFloat(value);
    }
}
