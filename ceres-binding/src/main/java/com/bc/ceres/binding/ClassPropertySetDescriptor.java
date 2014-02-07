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

package com.bc.ceres.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * A provider for property descriptors.
 *
 * @author Norman Fomferra
 * @since 0.14
 */
public class ClassPropertySetDescriptor extends DefaultPropertySetDescriptor {

    private final Class<?> type;
    private final PropertyDescriptorFactory propertyDescriptorFactory;
    private Map<String, Field> fields;

    public ClassPropertySetDescriptor(Class<?> type) {
        this(type, new DefaultPropertyDescriptorFactory());
    }

    public ClassPropertySetDescriptor(Class<?> type, PropertyDescriptorFactory propertyDescriptorFactory) {
        this.type = type;
        this.propertyDescriptorFactory = propertyDescriptorFactory;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> strings = getFields().keySet();
        return strings.toArray(new String[strings.size()]);
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) {
        PropertyDescriptor propertyDescriptor = super.getPropertyDescriptor(propertyName);
        if (propertyDescriptor == null) {
            Field field = getField(propertyName);
            if (field != null) {
                propertyDescriptor = propertyDescriptorFactory.createValueDescriptor(field);
                if (propertyDescriptor != null) {
                    addPropertyDescriptor(propertyDescriptor);
                }
            }
        }
        return propertyDescriptor;
    }

    protected Field getField(String propertyName) {
        return getFields().get(propertyName);
    }

    Map<String, Field> getFields() {
        if (fields == null) {
            fields = getFields(type);
        }
        return fields;
    }

    static Map<String, Field> getFields(Class<?> type) {
        return ClassScanner.getFields(type, new ClassScanner.FieldFilter() {
            @Override
            public boolean accept(Field field) {
                int modifiers = field.getModifiers();
                return !(Modifier.isFinal(modifiers)
                         || Modifier.isTransient(modifiers)
                         || Modifier.isStatic(modifiers));
            }
        });
    }
}
