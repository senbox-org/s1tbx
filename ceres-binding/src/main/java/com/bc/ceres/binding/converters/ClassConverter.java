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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassConverter implements Converter<Class> {

    private static final String ARRAY_SUFFIX = "[]";

    private static Map<String, Class<?>> primitiveTypes;

    static {
        primitiveTypes = new HashMap<>();
        primitiveTypes.put("char", Character.TYPE);
        primitiveTypes.put("boolean", Boolean.TYPE);
        primitiveTypes.put("byte", Byte.TYPE);
        primitiveTypes.put("short", Short.TYPE);
        primitiveTypes.put("int", Integer.TYPE);
        primitiveTypes.put("long", Long.TYPE);
        primitiveTypes.put("float", Float.TYPE);
        primitiveTypes.put("double", Double.TYPE);
        primitiveTypes.put("void", Void.TYPE);
    }

    private final ClassLoader classLoader;

    private final List<String> packageQualifiers = new ArrayList<>();

    public ClassConverter() {
        this(Thread.currentThread().getContextClassLoader());
    }

    private ClassConverter(ClassLoader classLoader) {
        this.classLoader = classLoader;
        addPackageQualifier("");
        addPackageQualifier("java.lang.");
        addPackageQualifier("java.util.");
    }

    @Override
    public Class<Class> getValueType() {
        return Class.class;
    }

    @Override
    public Class parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        Class<?> aClass = primitiveTypes.get(text);
        if (aClass != null) {
            return aClass;
        }

        Class type = null;
        for (String defaultPackageQualifier : packageQualifiers) {
            if (text.endsWith(ARRAY_SUFFIX)) {
                String typeString = defaultPackageQualifier + text.subSequence(0, text.length() - ARRAY_SUFFIX.length());
                String arrayTypeString = "[L" + typeString + ";";
                type = loadClass(arrayTypeString);
            } else {
                type = loadClass(defaultPackageQualifier + text);
            }
            if (type != null) {
                break;
            }
        }
        if (type == null) {
            throw new ConversionException(text);
        }
        return type;
    }

    private Class<?> loadClass(String typeString) {
        try {
            return Class.forName(typeString);
        } catch (ClassNotFoundException e) {
            try {
                return classLoader.loadClass(typeString);
            } catch (ClassNotFoundException cnfe2) {
                // ok
            }
        }
        return null;
    }

    @Override
    public String format(Class javaType) {
        if (javaType == null) {
            return "";
        }
        String name = javaType.getName();
        for (int i = 1; i < packageQualifiers.size(); i++) {
            String defaultPackageQualifier = packageQualifiers.get(i);
            if (name.startsWith("[L")) {
                name = name.substring(2, name.length() - 1) + "[]";
            }
            if (name.startsWith(defaultPackageQualifier)) {
                return name.substring(defaultPackageQualifier.length());
            }
        }
        return name;
    }

    protected void addPackageQualifier(String qualifier) {
        packageQualifiers.add(qualifier);
    }
}
