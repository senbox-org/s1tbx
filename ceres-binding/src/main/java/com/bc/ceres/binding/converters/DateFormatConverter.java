package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatConverter implements Converter {
    private DateFormat format;

    public DateFormatConverter() {
        this(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public DateFormatConverter(DateFormat format) {
        this.format = format;
    }

    public Class<?> getValueType() {
        return Date.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return format.parseObject(text);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        return format.format((Date) value);
    }
}
