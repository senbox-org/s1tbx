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

import com.bc.ceres.core.Assert;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of a property set descriptor.
 *
 * @author Norman Fomferra
 * @since 0.14
 */
public class DefaultPropertySetDescriptor implements PropertySetDescriptor {

    private Map<String, PropertyDescriptor> propertyDescriptors;

    public DefaultPropertySetDescriptor() {
    }

    public static PropertySetDescriptor createFromClass(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory) {
        Assert.notNull(valueType, "valueType");
        Assert.notNull(propertyDescriptorFactory, "propertyDescriptorFactory");

        DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
        Map<String, Field> fields = PropertyContainer.getPropertyFields(valueType);
        for (Field field : fields.values()) {
            PropertyDescriptor descriptor = propertyDescriptorFactory.createValueDescriptor(field);
            if (descriptor != null) {
                propertySetDescriptor.addPropertyDescriptor(descriptor);
            }
        }
        return propertySetDescriptor;
    }

    @Override
    public String[] getPropertyNames() {
        if (propertyDescriptors != null) {
            Set<String> strings = propertyDescriptors.keySet();
            return strings.toArray(new String[strings.size()]);
        }else {
            return new String[0];
        }
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) {
        if (propertyDescriptors != null) {
            return propertyDescriptors.get(propertyName);
        } else {
            return null;
        }
    }

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        if (propertyDescriptors == null) {
            propertyDescriptors = new LinkedHashMap<>();
        }
        propertyDescriptors.put(propertyDescriptor.getName(), propertyDescriptor);
    }
}
