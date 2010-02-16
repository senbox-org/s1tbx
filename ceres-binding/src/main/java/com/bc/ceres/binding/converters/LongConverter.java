package com.bc.ceres.binding.converters;

public class LongConverter extends NumberConverter<Long> {
    @Override
    public Class<Long> getValueType() {
        return Long.class;
    }

    @Override
    public Long parseNumber(String value) throws NumberFormatException {
        return Long.parseLong(value);
    }
}
