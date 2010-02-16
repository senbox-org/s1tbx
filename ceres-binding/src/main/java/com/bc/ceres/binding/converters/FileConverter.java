package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.io.File;

public class FileConverter implements Converter<File> {

    @Override
    public Class<File> getValueType() {
        return File.class;
    }

    @Override
    public File parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        return new File(text);
    }

    @Override
    public String format(File value) {
        if (value == null) {
            return "";
        }
        return ((File) value).getPath();
    }
}
