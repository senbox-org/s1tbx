package com.bc.ceres.binding.converters;


public class DoubleConverter extends NumberConverter<Double> {

    @Override
    public Class<Double> getValueType() {
        return Double.class;
    }

    @Override
    public Double parseNumber(String value) throws NumberFormatException {
        return Double.parseDouble(value);
    }
}
