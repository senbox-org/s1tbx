package com.bc.ceres.binding.converters;

public class ByteConverter extends NumberConverter {
    @Override
    public Class<?> getValueType() {
        return Byte.class;
    }

    @Override
    public Object parseNumber(String value) throws NumberFormatException {
        return Byte.parseByte(value);
    }
}
