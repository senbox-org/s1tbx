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
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public abstract class AbstractDomConverter implements DomConverter {

    private final Class<?> valueType;

    protected AbstractDomConverter(Class<?> valueType) {
        this.valueType = valueType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        final PropertySet propertySet = getPropertySet(value);
        final Property[] properties = propertySet.getProperties();
        for (Property property : properties) {
            final PropertyDescriptor descriptor = property.getDescriptor();
            if (!descriptor.isTransient()) {
                final DomConverter domConverter = getDomConverter(descriptor);
                if (domConverter != null) {
                    final DomElement childElement = parentElement.createChild(getNameOrAlias(property));
                    domConverter.convertValueToDom(property.getValue(), childElement);
                } else if (isArrayTypeWithNamedItems(descriptor)) {
                    final DomElement childElement;
                    if (descriptor.getItemsInlined()) {
                        childElement = parentElement;
                    } else {
                        childElement = parentElement.createChild(getNameOrAlias(property));
                    }
                    final Object array = property.getValue();
                    if (array != null) {
                        final int arrayLength = Array.getLength(array);
                        final PropertyDescriptor itemDescriptor = new PropertyDescriptor(descriptor.getItemAlias(),
                                                                                         descriptor.getType().getComponentType());
                        final DomConverter itemDomConverter = getDomConverter(itemDescriptor);
                        for (int i = 0; i < arrayLength; i++) {
                            final Object component = Array.get(array, i);
                            if (itemDomConverter != null) {
                                itemDomConverter.convertValueToDom(component,
                                                                   childElement.createChild(descriptor.getItemAlias()));
                            } else {
                                final Converter<?> itemConverter = getItemConverter(descriptor);
                                final DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                                convertValueToDomImpl(component, itemConverter, itemElement);
                            }
                        }
                    }
                } else {
                    final DomElement childElement = parentElement.createChild(getNameOrAlias(property));
                    final Class<?> type = property.getDescriptor().getType();
                    final Object childValue = property.getValue();
                    if (type.isInstance(childValue) && type != childValue.getClass() && !type.isEnum()) {
                        // childValue is an implementation of type and it's not of same type
                        // we have to store the implementation class in order to re-construct the object
                        // but only if type is not an enum.
                        childElement.setAttribute("class", childValue.getClass().getName());
                    }
                    Converter<?> converter = descriptor.getConverter();
                    if (converter == null) {
                        converter = ConverterRegistry.getInstance().getConverter(descriptor.getType());
                    }
                    convertValueToDomImpl(childValue, converter, childElement);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
            ValidationException {
        if (value == null) {
            Class<?> itemType = getValueType();
            final String itemClassName = parentElement.getAttribute("class");
            if (itemClassName != null) {  // implementation of an interface ?
                try {
                    itemType = Class.forName(itemClassName);
                } catch (ClassNotFoundException e) {
                    throw new ConversionException(e);
                }
            }
            value = createValueInstance(itemType);
        }

        final PropertySet propertySet = getPropertySet(value);
        final Map<String, List<Object>> inlinedArrays = new HashMap<String, List<Object>>();

        for (final DomElement child : parentElement.getChildren()) {
            convertDomChildToValue(child, propertySet, inlinedArrays);
        }

        if (!inlinedArrays.isEmpty()) {
            for (final Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
                final String propertyName = entry.getKey();
                final List<Object> valueList = entry.getValue();
                final Class<?> componentType = propertySet.getDescriptor(propertyName).getType().getComponentType();
                final Object array = Array.newInstance(componentType, valueList.size());

                propertySet.getProperty(propertyName).setValue(valueList.toArray((Object[]) array));
            }
        }

        return value;
    }

    private void convertDomChildToValue(DomElement child, PropertySet propertySet, Map<String, List<Object>> inlinedArrays) throws ConversionException, ValidationException {
        final String childName = child.getName();
        // todo - convert Java collections (nf - 02.04.2009)
        Property property = propertySet.getProperty(childName);

        if (property != null && property.getDescriptor().isTransient()) {
            return;
        }

        List<Object> inlinedArray = null;

        if (property == null) {
            // try to find property as inlined array element
            final Property[] properties = propertySet.getProperties();
            for (final Property p : properties) {
                final boolean inlined = p.getDescriptor().getItemsInlined();
                if (inlined) {
                    final String itemAlias = p.getDescriptor().getItemAlias();
                    if (childName.equals(itemAlias)) {
                        final String name = getNameOrAlias(p);
                        inlinedArray = inlinedArrays.get(name);
                        if (inlinedArray == null) {
                            inlinedArray = new ArrayList<Object>();
                            inlinedArrays.put(name, inlinedArray);
                        }
                        property = p;
                        break;
                    }
                }
            }
            if (property == null) {
                throw new ConversionException(String.format("Illegal element '%s'.", childName));
            }
        }

        final Object childValue;
        final PropertyDescriptor descriptor = property.getDescriptor();
        final DomConverter domConverter = getDomConverter(descriptor);
        if (domConverter != null) {
            childValue = domConverter.convertDomToValue(child, property.getValue());
            property.setValue(childValue);
        } else if (isArrayTypeWithNamedItems(descriptor)) {
            final Class<?> itemType = descriptor.getType().getComponentType();
            final Converter<?> itemConverter = getItemConverter(descriptor);
            if (inlinedArray != null) {
                Object item = convertDomToValueImpl(child, itemConverter, itemType);
                inlinedArray.add(item);
            } else {
                // if and only if an itemAlias is set, we parse the array element-wise
                final DomElement[] arrayElements = child.getChildren(descriptor.getItemAlias());
                final DomConverter itemDomConverter = getDomConverter(
                        new PropertyDescriptor(descriptor.getItemAlias(), itemType));
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
            }
        } else {
            childValue = convertDomToValueImpl(child,
                                               descriptor.getConverter(),
                                               descriptor.getType());
            property.setValue(childValue);
        }
    }

    /**
     * Gets an appropriate {@link com.bc.ceres.binding.PropertyContainer PropertyContainer} for the given value.
     *
     * @param value The value.
     * @return The value container.
     * @deprecated Since Ceres 0.12, replaced by {@link #getPropertySet(Object)}.
     */
    @Deprecated
    protected abstract PropertyContainer getPropertyContainer(Object value);

    /**
     * Gets an appropriate {@link com.bc.ceres.binding.PropertySet PropertySet} for the given value.
     * If {@code value} is already of type {@code PropertySet}, it is returned without any other action.
     *
     * @param value The value.
     * @return The property set.
     */
    protected abstract PropertySet getPropertySet(Object value);

    /**
     * Creates a {@link DomConverter} for a child element of a given value type.
     * This factory method is called by the {@link #convertValueToDom(Object, DomElement)} method
     * in order to convert a value for which no {@link com.bc.ceres.binding.Converter Converter}
     * was found.
     *
     * @param element   The element which requires the converter.
     * @param valueType The type of the value to be converted.
     * @return The converter.
     */
    protected abstract DomConverter createChildConverter(DomElement element, Class<?> valueType);

    /**
     * Gets a {@link DomConverter} for the given property descriptor.
     *
     * @param descriptor The descriptor which requires the converter.
     * @return The converter, may be {@code null}.
     */
    protected abstract DomConverter getDomConverter(PropertyDescriptor descriptor);

    protected void convertValueToDomImpl(Object value, Converter converter, DomElement element) throws
            ConversionException {
        if (converter != null) {
            final String text = converter.format(value);
            if (text != null && !text.isEmpty()) {
                element.setValue(text);
            }
        } else {
            if (value != null) {
                final PropertySet propertySet = getPropertySet(value);
                final Property[] properties = propertySet.getProperties();
                for (Property property : properties) {
                    final PropertyDescriptor descriptor = property.getDescriptor();
                    final DomConverter domConverter = getDomConverter(descriptor);
                    if (domConverter != null) {
                        final DomElement childElement = element.createChild(getNameOrAlias(property));
                        domConverter.convertValueToDom(property.getValue(), childElement);
                    } else if (isArrayTypeWithNamedItems(descriptor)) {
                        final DomElement childElement;
                        if (descriptor.getItemsInlined()) {
                            childElement = element;
                        } else {
                            childElement = element.createChild(getNameOrAlias(property));
                        }
                        final Object array = property.getValue();
                        if (array != null) {
                            final int arrayLength = Array.getLength(array);
                            final Converter<?> itemConverter = getItemConverter(descriptor);
                            for (int i = 0; i < arrayLength; i++) {
                                final Object component = Array.get(array, i);
                                final DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                                convertValueToDomImpl(component, itemConverter, itemElement);
                            }
                        }
                    } else {
                        final DomElement childElement = element.createChild(getNameOrAlias(property));
                        final Object childValue = property.getValue();
                        final Converter<?> converter1 = descriptor.getConverter();
                        convertValueToDomImpl(childValue, converter1, childElement);
                    }
                }
            }
        }
    }

    private Object convertDomToValueImpl(DomElement domElement,
                                         Converter<?> converter,
                                         Class<?> valueType) throws ConversionException, ValidationException {
        final Object childValue;

        if (converter == null) {
            converter = ConverterRegistry.getInstance().getConverter(valueType);
        }
        if (converter != null) {
            final String text = domElement.getValue();
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
            final DomConverter domConverter = createChildConverter(domElement, valueType);
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

    private static String getNameOrAlias(Property property) {
        final String alias = property.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return property.getDescriptor().getName();
    }

    private static Converter<?> getItemConverter(PropertyDescriptor descriptor) {
        Class<?> itemType = descriptor.getType().getComponentType();
        Converter<?> itemConverter = descriptor.getConverter();
        if (itemConverter == null) {
            itemConverter = ConverterRegistry.getInstance().getConverter(itemType);
        }
        return itemConverter;
    }

    private Object createValueInstance(Class<?> type) {
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

    private boolean isArrayTypeWithNamedItems(PropertyDescriptor descriptor) {
        return descriptor.getType().isArray() && descriptor.getItemAlias() != null && !descriptor.getItemAlias().isEmpty();
    }
}