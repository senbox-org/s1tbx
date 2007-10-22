package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class StringConverter implements com.bc.ceres.binding.Converter {
    public Class<?> getValueType() {
        return String.class;
    }

    public Object parse(String text) throws ConversionException {
        if (text == null) {
            throw new NullPointerException("text");
        }
        try {
            return URLDecoder.decode(text, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value.toString(), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return value.toString();
        }
    }
}
