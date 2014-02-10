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
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.ValidationException;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter implements DomConverter {

    private Class<?> valueType;
    private PropertySetDescriptor propertySetDescriptor;


    public DefaultDomConverter(Class<?> valueType) {
        this(valueType, new ClassPropertySetDescriptor(valueType));
    }

    public DefaultDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory) {
        this(valueType, new ClassPropertySetDescriptor(valueType, propertyDescriptorFactory));
    }

    public DefaultDomConverter(Class<?> valueType, PropertySetDescriptor propertySetDescriptor) {
        this.valueType = valueType;
        this.propertySetDescriptor = propertySetDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getValueType() {
        return valueType;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VALUE  -->  DOM
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        PropertySet propertySet = getPropertySet(value);
        convertPropertySetToDom(propertySet, parentElement);
    }


    private void convertPropertySetToDom(PropertySet propertySet, DomElement parentElement) throws ConversionException {
        Property[] properties = propertySet.getProperties();
        for (Property property : properties) {
            convertPropertyToDom(property, parentElement);
        }
    }

    private void convertPropertyToDom(Property property, DomElement parentElement) throws ConversionException {
        PropertyDescriptor descriptor = property.getDescriptor();
        if (descriptor.isTransient()) {
            return;
        }

        Object value = property.getValue();
        if (value == null) {
            return;
        }
        DomElement childElement = parentElement.createChild(getNameOrAlias(property));

        DomConverter domConverter = getDomConverter(descriptor);
        if (domConverter != null) {
            domConverter.convertValueToDom(value, childElement);
        } else if (isArrayTypeWithNamedItems(descriptor)) {
            int arrayLength = Array.getLength(value);
            PropertyDescriptor itemDescriptor = new PropertyDescriptor(descriptor.getItemAlias(),
                                                                       descriptor.getType().getComponentType());
            DomConverter itemDomConverter = getDomConverter(itemDescriptor);
            for (int i = 0; i < arrayLength; i++) {
                Object component = Array.get(value, i);
                if (itemDomConverter != null) {
                    itemDomConverter.convertValueToDom(component,
                                                       childElement.createChild(descriptor.getItemAlias()));
                } else {
                    Converter<?> itemConverter = getItemConverter(descriptor);
                    DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                    convertValueToDomImpl(component, itemElement, itemConverter);
                }
            }
        } else {
            Class<?> type = property.getDescriptor().getType();
            if (isExplicitClassNameRequired(type, value)) {
                // childValue is an implementation of type and it's not of same type
                // we have to store the implementation class in order to re-construct the object
                // but only if type is not an enum.
                childElement.setAttribute("class", value.getClass().getName());
            }
            Converter<?> converter = descriptor.getConverter();
            if (converter == null) {
                converter = ConverterRegistry.getInstance().getConverter(descriptor.getType());
            }
            convertValueToDomImpl(value, childElement, converter);
        }
    }

    protected void convertValueToDomImpl(Object value, DomElement element, Converter converter) throws
                                                                                                ConversionException {
        if (converter != null) {
            String text = converter.format(value);
            if (text != null && !text.isEmpty()) {
                element.setValue(text);
            }
        } else {
            if (value != null) {
                // todo - #1 inline property loop
                PropertySet propertySet = getPropertySet(value);
                Property[] properties = propertySet.getProperties();
                for (Property property : properties) {
                    PropertyDescriptor descriptor = property.getDescriptor();
                    DomConverter domConverter = getDomConverter(descriptor);
                    if (domConverter != null) {
                        if (property.getValue() != null) {
                            DomElement childElement = element.createChild(getNameOrAlias(property));
                            domConverter.convertValueToDom(property.getValue(), childElement);
                        }
                    } else if (isArrayTypeWithNamedItems(descriptor)) {
                        DomElement childElement = element.createChild(getNameOrAlias(property));
                        Object array = property.getValue();
                        if (array != null) {
                            int arrayLength = Array.getLength(array);
                            Converter<?> itemConverter = getItemConverter(descriptor);
                            for (int i = 0; i < arrayLength; i++) {
                                Object component = Array.get(array, i);
                                DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                                // todo - #2 use DomConverter instead of recursion
                                convertValueToDomImpl(component, itemElement, itemConverter);
                            }
                        }
                    } else {
                        DomElement childElement = element.createChild(getNameOrAlias(property));
                        Object childValue = property.getValue();
                        Converter<?> converter1 = descriptor.getConverter();
                        // todo - #2 use DomConverter instead of recursion
                        convertValueToDomImpl(childValue, childElement, converter1);
                    }
                }
            }
        }
    }


    private static String getNameOrAlias(Property property) {
        String alias = property.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return property.getDescriptor().getName();
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DOM  -->  VALUE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                   ValidationException {

        PropertySet propertySet;
        if (value == null) {
            Class<?> itemType = getValueType();
            String itemClassName = parentElement.getAttribute("class");
            if (itemClassName != null) {  // implementation of an interface ?
                try {
                    itemType = Class.forName(itemClassName);
                } catch (ClassNotFoundException e) {
                    throw new ConversionException(e);
                }
            }
            value = createValueInstance(itemType);
            propertySet = getPropertySet(value);
            propertySet.setDefaultValues();
        } else {
            propertySet = getPropertySet(value);
        }

        convertDomToPropertySet(parentElement, propertySet);

        return value;
    }

    private void convertDomToPropertySet(DomElement parentElement, PropertySet propertySet) throws ConversionException, ValidationException {
        for (DomElement childElement : parentElement.getChildren()) {
            convertDomChildToPropertySet(childElement, propertySet);
        }
    }

    private void convertDomChildToPropertySet(DomElement child, PropertySet propertySet) throws ConversionException,
                                                                                                ValidationException {
        // todo - possible bug: write test, assert to check for alias
        String childName = child.getName();
        Property property = propertySet.getProperty(childName);

        if (property == null) {
            return;
        }
        if (property.getDescriptor().isTransient()) {
            return;
        }

        Object childValue;
        PropertyDescriptor descriptor = property.getDescriptor();
        DomConverter domConverter = getDomConverter(descriptor);
        if (domConverter != null) {
            childValue = domConverter.convertDomToValue(child, property.getValue());
            property.setValue(childValue);
        } else if (isArrayTypeWithNamedItems(descriptor)) {
            Class<?> itemType = descriptor.getType().getComponentType();
            Converter<?> itemConverter = getItemConverter(descriptor);
            // if and only if an itemAlias is set, we parse the array element-wise
            DomElement[] arrayElements = child.getChildren(descriptor.getItemAlias());
            DomConverter itemDomConverter = getDomConverter(new PropertyDescriptor(descriptor.getItemAlias(), itemType));
            childValue = Array.newInstance(itemType, arrayElements.length);
            for (int i = 0; i < arrayElements.length; i++) {
                Object item;
                if (itemDomConverter != null) {
                    item = itemDomConverter.convertDomToValue(arrayElements[i], null);
                } else {
                    item = convertDomToValueImpl(arrayElements[i], itemConverter, itemType);
                }
                Array.set(childValue, i, item);
            }
            property.setValue(childValue);
        } else {
            childValue = convertDomToValueImpl(child,
                                               descriptor.getConverter(),
                                               descriptor.getType());
            property.setValue(childValue);
        }
    }

    private Object convertDomToValueImpl(DomElement domElement,
                                         Converter<?> converter,
                                         Class<?> valueType) throws ConversionException, ValidationException {
        Object childValue;

        if (converter == null) {
            converter = ConverterRegistry.getInstance().getConverter(valueType);
        }
        if (converter != null) {
            String text = domElement.getValue();
            if (text != null) {
                try {
                    childValue = converter.parse(text);
                } catch (ConversionException e) {
                    throw new ConversionException(
                            "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
                }
            } else {
                childValue = null;
            }
        } else {
            DomConverter domConverter = createDomConverter(domElement, valueType);
            try {
                childValue = domConverter.convertDomToValue(domElement, null);
            } catch (ValidationException e) {
                throw new ValidationException(
                        "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
            } catch (ConversionException e) {
                throw new ConversionException(
                        "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
            }
        }

        return childValue;
    }

    protected DomConverter createDomConverter(DomElement element, Class<?> valueType) {
        String className = element.getAttribute("class");
        if (className != null && !className.trim().isEmpty()) {
            try {
                valueType = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        PropertyDescriptor propertyDescriptor = getPropertyDescriptorByNameOrAlias(element);
        if (propertyDescriptor != null) {
            PropertySetDescriptor propertySetDescriptor = propertyDescriptor.getPropertySetDescriptor();
            if (propertySetDescriptor != null) {
                return new DefaultDomConverter(valueType, propertySetDescriptor);
            }
        }

        return new DefaultDomConverter(valueType);
    }

    protected Object createValueInstance(Class<?> type) {
        if (type == Map.class) {
            // retain add-order of elements
            return new LinkedHashMap();
        } else if (type == SortedMap.class) {
            return new TreeMap();
        } else {
            Object childValue;
            try {
                childValue = type.newInstance();
            } catch (Throwable t) {
                throw new RuntimeException(
                        String.format("Failed to create instance of %s (default constructor missing?).", type.getName()),
                        t);
            }
            return childValue;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // common
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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

    protected DomConverter getDomConverter(PropertyDescriptor descriptor) {
        DomConverter domConverter = descriptor.getDomConverter();
        if (domConverter != null) {
            return domConverter;
        }

        domConverter = DomConverterRegistry.getInstance().getConverter(descriptor.getType());
        if (domConverter != null) {
            return domConverter;
        }

        PropertySetDescriptor childPropertySetDescriptor = descriptor.getPropertySetDescriptor();
        if (childPropertySetDescriptor != null) {
            domConverter = new DefaultDomConverter(descriptor.getType(), childPropertySetDescriptor);
        }
        return domConverter;
    }

    private PropertyDescriptor getPropertyDescriptorByNameOrAlias(DomElement element) {
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

    private static Converter<?> getItemConverter(PropertyDescriptor descriptor) {
        Converter<?> itemConverter = descriptor.getConverter();
        if (itemConverter == null) {
            Class<?> itemType = descriptor.getType().getComponentType();
            itemConverter = ConverterRegistry.getInstance().getConverter(itemType);
        }
        return itemConverter;
    }

    private boolean isArrayTypeWithNamedItems(PropertyDescriptor descriptor) {
        return descriptor.getType().isArray() && descriptor.getItemAlias() != null && !descriptor.getItemAlias().isEmpty();
    }

    private boolean isExplicitClassNameRequired(Class<?> type, Object childValue) {
        return type.isInstance(childValue) && type != childValue.getClass() && !type.isEnum();
    }


}