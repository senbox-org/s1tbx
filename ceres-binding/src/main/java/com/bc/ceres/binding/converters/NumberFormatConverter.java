package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

import java.text.NumberFormat;

public class NumberFormatConverter implements com.bc.ceres.binding.Converter<Object> {

    private NumberFormat format;

    public NumberFormatConverter(NumberFormat format) {
        this.format = format;
    }

    @Override
    public Class<?> getValueType() {
        return Number.class;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return format.parseObject(text);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(Object value) {
        return format.format(value);
    }
}
