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

import com.bc.ceres.binding.ClassPropertySetDescriptor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.PropertySetDescriptor;

import java.util.Map;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter extends AbstractDomConverter {

    private final PropertySetDescriptor propertySetDescriptor;

    public DefaultDomConverter(Class<?> valueType) {
        this(valueType, new ClassPropertySetDescriptor(valueType));
    }

    public DefaultDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory) {
        this(valueType, new ClassPropertySetDescriptor(valueType, propertyDescriptorFactory));
    }

    public DefaultDomConverter(Class<?> valueType, PropertySetDescriptor propertySetDescriptor) {
        super(valueType);
        this.propertySetDescriptor = propertySetDescriptor;
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertySet getPropertySet(Object value) {
        PropertySet propertySet;
        if (value instanceof PropertySet) {
            propertySet = (PropertySet) value;
        } else if (value instanceof Map) {
            // todo - this wrong for recursive calls with depth > 0
            propertySet = PropertyContainer.createMapBacked((Map) value, propertySetDescriptor);
        } else {
            // todo - this wrong for recursive calls with depth > 0
            propertySet = PropertyContainer.createObjectBacked(value, propertySetDescriptor);
        }
        return propertySet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DomConverter createChildConverter(DomElement element, Class<?> valueType) {
        String className = element.getAttribute("class");
        if (className != null && !className.trim().isEmpty()) {
            try {
                valueType = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(element);
        if (propertyDescriptor != null) {
            PropertySetDescriptor propertySetDescriptor = propertyDescriptor.getPropertySetDescriptor();
            if (propertySetDescriptor != null) {
                return new DefaultDomConverter(valueType, propertySetDescriptor);
            }
        }

        return new DefaultDomConverter(valueType);
    }

    private PropertyDescriptor getPropertyDescriptor(DomElement element) {
        String elementName = element.getName();
        PropertyDescriptor propertyDescriptor = propertySetDescriptor.getPropertyDescriptor(elementName);
        if (propertyDescriptor == null) {
            propertyDescriptor = getPropertyDescriptorByAlias(elementName);
        }
        return propertyDescriptor;
    }

    private PropertyDescriptor getPropertyDescriptorByAlias(String elementName) {
        // Note: naive loop may be accelerated by a constant Map since propertySetDescriptor is constant (nf)
        for (String propertyName : propertySetDescriptor.getPropertyNames()) {
            PropertyDescriptor propertyDescriptor = propertySetDescriptor.getPropertyDescriptor(propertyName);
            if (elementName.equals(propertyDescriptor.getAlias())) {
                return propertyDescriptor;
            }
        }
        return null;
    }

}