package com.bc.ceres.binding.converters;


public class IntegerConverter extends NumberConverter<Integer> {
    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }

    @Override
    public Integer parseNumber(String value) throws NumberFormatException {
        return Integer.parseInt(value);
    }
}
