package com.bc.ceres.binding;

public interface Converter {
    Class<?> getValueType();
    Object parse(String text) throws ConversionException;
    String format(Object value) throws ConversionException;
}
