package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValueRange;

public class IntervalConverter implements com.bc.ceres.binding.Converter<ValueRange> {
    @Override
    public Class<ValueRange> getValueType() {
        return ValueRange.class;
    }

    @Override
    public ValueRange parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return ValueRange.parseValueRange(text);
    }

    @Override
    public String format(ValueRange value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
