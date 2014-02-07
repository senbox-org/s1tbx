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

package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter extends AbstractDomConverter {

    private final PropertyDescriptorFactory propertyDescriptorFactory;

    public DefaultDomConverter(Class<?> valueType) {
        this(valueType, null);
    }

    public DefaultDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory) {
        super(valueType);
        this.propertyDescriptorFactory = propertyDescriptorFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertySet getPropertySet(Object value) {
        if (value instanceof PropertySet) {
            return (PropertySet) value;
        }
        final PropertySet propertySet;
        if (propertyDescriptorFactory != null) {
            propertySet = PropertyContainer.createObjectBacked(value, propertyDescriptorFactory);
        } else {
            propertySet = PropertyContainer.createObjectBacked(value);
        }
        return propertySet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter createChildConverter(DomElement element, Class<?> valueType) {
        final String className = element.getAttribute("class");
        if (className != null && !className.trim().isEmpty()) {
            try {
                valueType = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return new DefaultDomConverter(valueType, propertyDescriptorFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter getDomConverter(PropertyDescriptor descriptor) {
        DomConverter domConverter = descriptor.getDomConverter();
        if (domConverter == null) {
            domConverter = DomConverterRegistry.getInstance().getConverter(descriptor.getType());
        }
        return domConverter;
    }

}