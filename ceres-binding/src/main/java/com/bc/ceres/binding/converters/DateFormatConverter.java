package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatConverter implements Converter<Date>{
    private DateFormat format;

    public DateFormatConverter() {
        this(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public DateFormatConverter(DateFormat format) {
        this.format = format;
    }

    @Override
    public Class<Date> getValueType() {
        return Date.class;
    }

    @Override
    public Date parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return format.parse(text);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(Date value) {
        if (value == null) {
            return "";
        }
        return format.format( value);
    }
}
