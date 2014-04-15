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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.DefaultPropertyDescriptorFactory;
import com.bc.ceres.binding.DefaultPropertySetDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.Assert;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter implements DomConverter {

    private static final String CLASS_ATTRIBUTE_NAME = "class";

    private final Class<?> valueType;
    private PropertySetDescriptor propertySetDescriptor;
    private PropertyDescriptorFactory propertyDescriptorFactory;

    public DefaultDomConverter(Class<?> valueType) {
        this(valueType, new DefaultPropertyDescriptorFactory());
    }

    public DefaultDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory) {
        this(valueType, propertyDescriptorFactory, null);
    }

    public DefaultDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory, PropertySetDescriptor propertySetDescriptor) {
        Assert.notNull(valueType, "valueType");
        Assert.notNull(propertyDescriptorFactory, "propertyDescriptorFactory");
        this.valueType = valueType;
        this.propertyDescriptorFactory = propertyDescriptorFactory;
        this.propertySetDescriptor = propertySetDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    public PropertyDescriptorFactory getPropertyDescriptorFactory() {
        return propertyDescriptorFactory;
    }

    public PropertySetDescriptor getPropertySetDescriptor() {
        if (propertySetDescriptor == null) {
            propertySetDescriptor = DefaultPropertySetDescriptor.createFromClass(valueType, propertyDescriptorFactory);
        }
        return propertySetDescriptor;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VALUE  -->  DOM
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        PropertySet propertySet = getPropertySet(value);
        for (Property property : propertySet.getProperties()) {
            convertPropertyToDom(property, parentElement);
        }
    }

    private void convertPropertyToDom(Property property, DomElement parentElement) throws ConversionException {
        PropertyDescriptor descriptor = property.getDescriptor();
        if (descriptor.isTransient() || descriptor.isDeprecated()) {
            return;
        }

        Object value = property.getValue();
        if (value == null) {
            return;
        }

        DomElement childElement = parentElement.createChild(getNameOrAlias(property));
        Class<?> type = property.getDescriptor().getType();
        Class<?> actualType = value.getClass();
        if (isExplicitClassNameRequired(type, value)) {
            // childValue is an implementation of type and it's not of same type
            // we have to store the implementation class in order to re-construct the object
            // but only if type is not an enum.
            childElement.setAttribute(CLASS_ATTRIBUTE_NAME, actualType.getName());
        }

        ChildConverter childConverter = findChildConverter(descriptor, actualType);
        if (childConverter == null) {
            throw new ConversionException(String.format("Don't know how to convert property '%s'", childElement.getName()));
        }

        childConverter.convertValueToDom(value, childElement);
    }

    private static String getNameOrAlias(Property property) {
        String alias = property.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return property.getDescriptor().getName();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DOM  -->  VALUE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {

        PropertySet propertySet;
        if (value == null) {
            value = createValueInstance(parentElement, getValueType());
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

    private void convertDomChildToPropertySet(DomElement child, PropertySet propertySet) throws ConversionException, ValidationException {

        String childName = child.getName();
        Property property = propertySet.getProperty(childName);

        if (property == null) {
            throw new ConversionException(String.format("Unknown element '%s'", childName));
        }

        if (property.getDescriptor().isTransient()) {
            return;
        }

        convertDomChildToProperty(child, property);
    }

    private void convertDomChildToProperty(DomElement childElement, Property property) throws ConversionException, ValidationException {
        PropertyDescriptor descriptor = property.getDescriptor();
        Object currentValue = property.getValue();
        Class<?> actualType = getActualType(childElement, currentValue != null ? currentValue.getClass() : null, false);
        ChildConverter childConverter = findChildConverter(descriptor, actualType);
        if (childConverter == null) {
            throw new ConversionException(String.format("Don't know how to convert element '%s'", childElement.getName()));
        }
        Object value = childConverter.convertDomToValue(childElement, currentValue);
        property.setValue(value);
    }

    private Object createValueInstance(DomElement parentElement, Class<?> defaultType) throws ConversionException {
        Class<?> itemType = getActualType(parentElement, defaultType, true);
        return createValueInstance(itemType);
    }

    private Class<?> getActualType(DomElement parentElement, Class<?> defaultType, boolean failIfNotFound) throws ConversionException {
        Class<?> actualType;
        String className = parentElement.getAttribute(CLASS_ATTRIBUTE_NAME);
        if (className != null) {  // implementation of an interface ?
            try {
                actualType = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                if (failIfNotFound) {
                    throw new ConversionException(e);
                }
                // This is ok, type info may not be used at all.
                actualType = defaultType;
            }
        } else {
            actualType = defaultType;
        }
        return actualType;
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
                throw new RuntimeException(String.format("Failed to create instance of %s (default constructor missing?).", type.getName()), t);
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
            propertySet = PropertyContainer.createMapBacked((Map) value, getPropertySetDescriptor());
        } else if (value.getClass().equals(getValueType())) {
            propertySet = PropertyContainer.createObjectBacked(value, getPropertySetDescriptor());
        } else {
            propertySet = PropertyContainer.createObjectBacked(value, getPropertyDescriptorFactory());
        }
        return propertySet;
    }

    /**
     * Called to create a new DOM converter for a (child) property.
     * May be overridden by subclasses. The default implementation returns an instance of this class.
     *
     * @param valueType                 The value type
     * @param propertyDescriptorFactory The property descriptor factory.
     * @param propertySetDescriptor     The property set descriptor.
     * @return a "local" DOM converter or {@code null}.
     */
    protected DomConverter createChildDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory, PropertySetDescriptor propertySetDescriptor) {
        return new DefaultDomConverter(valueType, propertyDescriptorFactory, propertySetDescriptor);
    }

    /**
     * Called to find a "local" DOM converter for a (child) property.
     * May be overridden by subclasses. The default implementation returns {@code null}.
     *
     * @param descriptor The property descriptor
     * @return a "local" DOM converter or {@code null}.
     */
    protected DomConverter findChildDomConverter(PropertyDescriptor descriptor) {
        return null;
    }

    private ChildConverter findChildConverter(PropertyDescriptor descriptor, Class<?> actualType) {
        DomConverter domConverter = findChildDomConverter(descriptor);
        if (domConverter != null) {
            return new ComplexChildConverter(domConverter);
        }

        domConverter = descriptor.getDomConverter();
        if (domConverter != null) {
            return new ComplexChildConverter(domConverter);
        }

        PropertySetDescriptor psd = descriptor.getPropertySetDescriptor();
        if (psd != null) {
            return new ComplexChildConverter(createChildDomConverter(descriptor.getType(), getPropertyDescriptorFactory(), psd));
        }

        if (descriptor.getType().isArray()) {
            boolean hasItemAlias = descriptor.getItemAlias() != null && !descriptor.getItemAlias().isEmpty();
            if (hasItemAlias) {
                String itemName = descriptor.getItemAlias();
                Class<?> itemType = descriptor.getType().getComponentType();
                PropertyDescriptor itemDescriptor = new PropertyDescriptor(itemName, itemType);
                ChildConverter itemChildConverter = findChildConverter(itemDescriptor, actualType != null ? actualType.getComponentType() : null);
                if (itemChildConverter != null) {
                    return new ArrayToDomConverter(itemName, itemType, itemChildConverter);
                }
            }
        }

        Converter converter = descriptor.getConverter();
        if (converter != null) {
            return new SingleValueChildConverter(converter);
        }

        ChildConverter globalChildConverter = findGlobalChildConverter(descriptor.getType());
        if (globalChildConverter != null) {
            return globalChildConverter;
        }

        // up to this point we tried to exploit property descriptor attributes
        // but didn't find a converter. Now we ask the actual type, if any.

        if (actualType != null) {
            if (!actualType.equals(descriptor.getType())) {
                ChildConverter childConverter = findGlobalChildConverter(actualType);
                if (childConverter != null) {
                    return childConverter;
                }
            }
            return createChildConverter(actualType);
        } else if (isInstantiable(descriptor.getType())) {
            return createChildConverter(descriptor.getType());
        }

        return null;
    }

    private boolean isInstantiable(Class<?> type) {
        // Note: we don't check for no-arg constructor here, because we want Java to throw a runtime exception
        return !type.isInterface()
               && !Modifier.isAbstract(type.getModifiers())
               && !type.isEnum()
               && !type.isArray()
               && Modifier.isPublic(type.getModifiers());
    }

    private boolean isExplicitClassNameRequired(Class<?> type, Object value) {
        return type.isInstance(value)
               && type != value.getClass()
               && isInstantiable(value.getClass());
    }

    private ChildConverter createChildConverter(Class<?> actualType) {
        PropertySetDescriptor actualTypePsd = DefaultPropertySetDescriptor.createFromClass(actualType, getPropertyDescriptorFactory());
        return new ComplexChildConverter(createChildDomConverter(actualType, getPropertyDescriptorFactory(), actualTypePsd));
    }

    private static ChildConverter findGlobalChildConverter(Class<?> type) {
        Converter converter = ConverterRegistry.getInstance().getConverter(type);
        if (converter != null) {
            return new SingleValueChildConverter(converter);
        }

        DomConverter domConverter = DomConverterRegistry.getInstance().getConverter(type);
        if (domConverter != null) {
            return new ComplexChildConverter(domConverter);
        }

        return null;
    }


    private static interface ChildConverter {
        void convertValueToDom(Object value, DomElement childElement) throws ConversionException;

        Object convertDomToValue(DomElement childElement, Object value) throws ConversionException, ValidationException;
    }

    private static class SingleValueChildConverter implements ChildConverter {
        final Converter converter;

        private SingleValueChildConverter(Converter converter) {
            this.converter = converter;
        }

        @Override
        public void convertValueToDom(Object value, DomElement childElement) throws ConversionException {
            String text = converter.format(value);
            if (text != null && !text.isEmpty()) {
                childElement.setValue(text);
            }
        }

        @Override
        public Object convertDomToValue(DomElement childElement, Object value) throws ConversionException {
            String text = childElement.getValue();
            if (text != null) {
                return converter.parse(text);
            } else {
                return null;
            }
        }
    }

    private static class ComplexChildConverter implements ChildConverter {
        final DomConverter domConverter;

        private ComplexChildConverter(DomConverter domConverter) {
            this.domConverter = domConverter;
        }

        @Override
        public void convertValueToDom(Object value, DomElement childElement) throws ConversionException {
            domConverter.convertValueToDom(value, childElement);
        }

        @Override
        public Object convertDomToValue(DomElement childElement, Object value) throws ConversionException, ValidationException {
            return domConverter.convertDomToValue(childElement, value);
        }
    }

    private static class ArrayToDomConverter implements ChildConverter {
        private final String itemName;
        private final Class<?> itemType;
        final ChildConverter itemConverter;

        private ArrayToDomConverter(String itemName, Class<?> itemType, ChildConverter itemConverter) {
            this.itemName = itemName;
            this.itemType = itemType;
            this.itemConverter = itemConverter;
        }

        @Override

        public void convertValueToDom(Object value, DomElement childElement) throws ConversionException {
            int arrayLength = Array.getLength(value);
            for (int i = 0; i < arrayLength; i++) {
                Object item = Array.get(value, i);
                DomElement itemDomElement = childElement.createChild(itemName);
                itemConverter.convertValueToDom(item, itemDomElement);
            }
        }

        @Override
        public Object convertDomToValue(DomElement childElement, Object value) throws ConversionException, ValidationException {
            // if and only if an itemAlias is set, we parse the array element-wise
            DomElement[] itemElements = childElement.getChildren(itemName);
            if (value == null || itemElements.length != Array.getLength(value)) {
                value = Array.newInstance(itemType, itemElements.length);
            } else if (value.getClass().getComponentType() == null) {
                throw new ConversionException(String.format("Incompatible value type: array of type '%s' expected", itemType.getName()));
            } else if (!itemType.isAssignableFrom(value.getClass().getComponentType())) {
                throw new ConversionException(String.format("Incompatible array item type: expected '%s', got '%s'", itemType.getName(), value.getClass().getComponentType()));
            }
            for (int i = 0; i < itemElements.length; i++) {
                Object item = itemConverter.convertDomToValue(itemElements[i], null);
                Array.set(value, i, item);
            }
            return value;
        }
    }


}