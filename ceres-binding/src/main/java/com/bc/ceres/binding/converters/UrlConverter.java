package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlConverter implements Converter<URL> {

    @Override
    public Class<URL> getValueType() {
        return URL.class;
    }

    @Override
    public URL parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(URL value) {
        if (value == null) {
            return "";
        }
        return value.toExternalForm();
    }
}