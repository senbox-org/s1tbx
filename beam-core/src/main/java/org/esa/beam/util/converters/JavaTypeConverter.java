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

package org.esa.beam.util.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

public class JavaTypeConverter implements Converter<Class> {
    private static final String[] DEFAULT_PACKAGE_QUALIFIERS = new String[]{
            "",
            "java.lang.",
            "java.util.",
            "com.vividsolutions.jts.geom.",
    };

    @Override
    public Class<Class> getValueType() {
        return Class.class;
    }

    @Override
    public Class parse(String text) throws ConversionException {
        Class type = null;
        for (String defaultPackageQualifier : DEFAULT_PACKAGE_QUALIFIERS) {
            try {
                type = getClass().getClassLoader().loadClass(defaultPackageQualifier + text);
                break;
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        if (type == null) {
            throw new ConversionException(text);
        }
        return type;
    }

    @Override
    public String format(Class javaType) {
        final String name = javaType.getName();
        for (int i = 1; i < DEFAULT_PACKAGE_QUALIFIERS.length; i++) {
            String defaultPackageQualifier = DEFAULT_PACKAGE_QUALIFIERS[i];
            if (name.startsWith(defaultPackageQualifier)) {
                return name.substring(defaultPackageQualifier.length());
            }
        }
        return name;
    }
}
