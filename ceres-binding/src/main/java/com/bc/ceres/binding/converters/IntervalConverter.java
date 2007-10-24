package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValueRange;

public class IntervalConverter implements com.bc.ceres.binding.Converter {
    public Class<?> getValueType() {
        return ValueRange.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return ValueRange.parseValueRange(text);
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
