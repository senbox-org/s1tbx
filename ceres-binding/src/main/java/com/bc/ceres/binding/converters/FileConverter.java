/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
