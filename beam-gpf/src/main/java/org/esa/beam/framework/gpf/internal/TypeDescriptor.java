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

package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TypeDescriptor {

    private final Class<?> type;
    private Map<Field, Parameter> parameterMap;

    public TypeDescriptor(Class<?> type) {
        this.type = type;
        parameterMap = new HashMap<Field, Parameter>();
        processAnnotationsRec(type);
    }

    public final Class<?> getType() {
        return type;
    }

    public final Map<Field, Parameter> getParameterMap() {
        return parameterMap;
    }

    private void processAnnotationsRec(Class<?> type) {
        final Class<?> superclass = type.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            processAnnotationsRec(superclass);
        }
        final Field[] declaredFields = type.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            final Parameter parameter = declaredField.getAnnotation(Parameter.class);
            if (parameter != null) {
                parameterMap.put(declaredField, parameter);
            }
        }
    }
}