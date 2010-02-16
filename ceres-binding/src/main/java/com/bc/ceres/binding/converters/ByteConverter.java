package com.bc.ceres.binding.converters;

public class ByteConverter extends NumberConverter<Byte> {
    @Override
    public Class<Byte> getValueType() {
        return Byte.class;
    }

    @Override
    public Byte parseNumber(String value) throws NumberFormatException {
        return Byte.parseByte(value);
    }
}
