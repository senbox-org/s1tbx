package com.bc.ceres.binding.converters;

public class FloatConverter extends NumberConverter<Float> {

    @Override
    public Class<Float> getValueType() {
        return Float.class;
    }

    @Override
    public Float parseNumber(String value) throws NumberFormatException {
        return Float.parseFloat(value);
    }
}
