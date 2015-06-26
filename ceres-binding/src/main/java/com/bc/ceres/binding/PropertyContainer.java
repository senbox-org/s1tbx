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

import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A convenience implementation of the {@link PropertySet} interface.
 * {@link PropertyContainer} is basically an implementation of the <i>Property List</i> design pattern.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class PropertyContainer implements PropertySet {

    private final Map<String, Property> propertyMap;
    private final List<Property> propertyList;
    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * Constructs a new, empty property container.
     */
    public PropertyContainer() {
        propertyMap = new HashMap<>(10);
        propertyList = new ArrayList<>(10);
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Creates a property container for the given object.
     * The factory method will not modify the object, thus not setting any default values.
     *
     * @param object the backing object
     * @return The property container.
     */
    public static PropertyContainer createObjectBacked(Object object) {
        return createObjectBacked(object,
                new DefaultPropertyDescriptorFactory());
    }

    /**
     * Creates a property container for the given object.
     * The factory method will not modify the object, thus not setting any default values.
     *
     * @param object            the backing object
     * @param descriptorFactory a factory used to create {@link PropertyDescriptor}s of the fields of the object's type
     * @return The property container.
     */
    public static PropertyContainer createObjectBacked(Object object,
                                                       PropertyDescriptorFactory descriptorFactory) {
        return createForFields(object.getClass(),
                descriptorFactory,
                new ObjectBackedPropertyAccessorFactory(object),
                false);
    }

    public static PropertyContainer createObjectBacked(Object object, PropertySetDescriptor propertySetDescriptor) {

        Map<String, Field> fields = getPropertyFields(object.getClass());

        PropertyContainer propertySet = new PropertyContainer();
        for (String propertyName : propertySetDescriptor.getPropertyNames()) {
            PropertyDescriptor propertyDescriptor = propertySetDescriptor.getPropertyDescriptor(propertyName);
            Field field = fields.get(propertyDescriptor.getName());
            if (field != null) {
                propertyDescriptor.initDefaults();
                propertySet.addProperty(new Property(propertyDescriptor, new ClassFieldAccessor(object, field)));
            }
        }
        return propertySet;
    }

    /**
     * Creates a property container for a map backing the values.
     * The properties are derived from the current map entries.
     *
     * @param map the map which backs the values
     * @return The property container.
     */
    public static PropertyContainer createMapBacked(Map<String, Object> map) {
        PropertyContainer propertyContainer = new PropertyContainer();
        for (Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            final PropertyDescriptor propertyDescriptor = new PropertyDescriptor(name, value.getClass());
            propertyDescriptor.initDefaults();
            propertyContainer.addProperty(new Property(propertyDescriptor, new MapEntryAccessor(map, name)));
        }
        return propertyContainer;
    }

    /**
     * Creates a property container for a map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map                   the map which backs the values
     * @param propertySetDescriptor A descriptor the property set to be created.
     * @return The property container.
     */
    public static PropertyContainer createMapBacked(Map<String, Object> map, PropertySetDescriptor propertySetDescriptor) {
        PropertyContainer propertySet = new PropertyContainer();
        for (String propertyName : propertySetDescriptor.getPropertyNames()) {
            PropertyDescriptor propertyDescriptor = propertySetDescriptor.getPropertyDescriptor(propertyName);
            propertyDescriptor.initDefaults();
            propertySet.addProperty(new Property(propertyDescriptor, new MapEntryAccessor(map, propertyName)));
        }
        return propertySet;
    }

    /**
     * Creates a property container for the given template type and map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map          the map which backs the values
     * @param templateType the template type
     * @return The property container.
     */
    public static PropertyContainer createMapBacked(Map<String, Object> map, Class<?> templateType) {
        return createMapBacked(map,
                templateType,
                new DefaultPropertyDescriptorFactory());
    }

    /**
     * Creates a property container for the given template type and map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map               the map which backs the values
     * @param templateType      the template type
     * @param descriptorFactory a factory used to create {@link PropertyDescriptor}s of the fields of the template type
     * @return The property container.
     */
    public static PropertyContainer createMapBacked(Map<String, Object> map, Class<?> templateType,
                                                    PropertyDescriptorFactory descriptorFactory) {
        return createForFields(templateType,
                descriptorFactory,
                new MapBackedPropertyAccessorFactory(map), false);
    }

    /**
     * Creates a property container for the given template type.
     * All properties will have their values set to default values (if specified).
     *
     * @param templateType the template type
     * @return The property container.
     */
    public static PropertyContainer createValueBacked(Class<?> templateType) {
        return createValueBacked(templateType,
                new DefaultPropertyDescriptorFactory());
    }

    /**
     * Creates a property container for the given template type.
     * All properties will have their values set to default values (if specified).
     *
     * @param templateType      the template class used to derive the descriptors from
     * @param descriptorFactory a factory used to create {@link PropertyDescriptor}s of the fields of the template type
     * @return The property container.
     */
    public static PropertyContainer createValueBacked(Class<?> templateType,
                                                      PropertyDescriptorFactory descriptorFactory) {
        return createForFields(templateType,
                descriptorFactory,
                new ValueBackedPropertyAccessorFactory(),
                true);
    }

    /**
     * Creates a property container for the given template type. Properties are generated
     * using the {@code descriptorFactory} and the {@code accessorFactory} which
     * are called for each non-static and non-transient class field.
     *
     * @param type              The type that provides the fields.
     * @param descriptorFactory The property descriptor factory.
     * @param accessorFactory   The property accessor factory.
     * @param initValues        If {@code true}, properties are initialised by their default values, if specified.
     * @return The property container.
     */
    public static PropertyContainer createForFields(Class<?> type,
                                                    PropertyDescriptorFactory descriptorFactory,
                                                    PropertyAccessorFactory accessorFactory,
                                                    boolean initValues) {
        PropertyContainer container = new PropertyContainer();
        collectProperties(type, descriptorFactory, accessorFactory, container);
        if (initValues) {
            container.setDefaultValues();
        }
        return container;
    }

    static Map<String, Field> getPropertyFields(Class<?> type) {
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

    @Override
    public Property[] getProperties() {
        return propertyList.toArray(new Property[propertyList.size()]);
    }

    @Override
    public boolean isPropertyDefined(String name) {
        return propertyMap.containsKey(name);
    }

    @Override
    public Property getProperty(String name) {
        Assert.notNull(name, "name");
        return propertyMap.get(name);
    }

    @Override
    public void addProperty(Property property) {
        if (propertyMap.put(property.getName(), property) != property) {
            final String alias = property.getDescriptor().getAlias();
            if (alias != null && !alias.isEmpty()) {
                propertyMap.put(alias, property);
            }
            propertyList.add(property);
            property.setContainer(this);
        }
    }

    @Override
    public void addProperties(Property... properties) {
        for (Property property : properties) {
            addProperty(property);
        }
    }

    @Override
    public void removeProperty(Property property) {
        if (property != null && property.getName() != null && propertyMap.remove(property.getName()) != null) {
            final String alias = property.getDescriptor().getAlias();
            if (alias != null && !alias.isEmpty()) {
                propertyMap.remove(alias);
            }
            propertyList.remove(property);
            property.setContainer(null);
        }
    }

    @Override
    public void removeProperties(Property... properties) {
        for (Property property : properties) {
            removeProperty(property);
        }
    }

    @Override
    public <T> T getValue(String name) {
        final Property property = getProperty(name);
        if (property == null) {
            return null;
        }
        //noinspection unchecked
        return (T) property.getValue();
    }

    @Override
    public void setValue(String name, Object value) throws IllegalArgumentException {
        try {
            getProperty(name).setValue(value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public PropertyDescriptor getDescriptor(String name) {
        final Property property = getProperty(name);
        if (property == null) {
            return null;
        }
        return getProperty(name).getDescriptor();
    }

    @Override
    public void setDefaultValues() throws IllegalStateException {
        for (final Property property : getProperties()) {
            final PropertyDescriptor descriptor = property.getDescriptor();
            if (descriptor.getDefaultValue() != null) {
                try {
                    property.setValue(descriptor.getDefaultValue());
                } catch (ValidationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        getPropertyChangeSupport().addPropertyChangeListener(l);
    }

    @Override
    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        getPropertyChangeSupport().addPropertyChangeListener(name, l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        getPropertyChangeSupport().removePropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        getPropertyChangeSupport().removePropertyChangeListener(name, l);
    }

    PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    private static void collectProperties(Class<?> type, PropertyDescriptorFactory descriptorFactory, PropertyAccessorFactory accessorFactory, PropertySet propertySet) {
        Map<String, Field> fields = getPropertyFields(type);
        for (String key : fields.keySet()) {
            Field field = fields.get(key);
            PropertyDescriptor descriptor = descriptorFactory.createValueDescriptor(field);
            if (descriptor != null) {
                descriptor.initDefaults();
                PropertyAccessor accessor = accessorFactory.createValueAccessor(field);
                if (accessor != null) {
                    propertySet.addProperty(new Property(descriptor, accessor));
                }
            }
        }
    }

    private static class ObjectBackedPropertyAccessorFactory implements PropertyAccessorFactory {

        private final Object object;

        private ObjectBackedPropertyAccessorFactory(Object object) {
            this.object = object;
        }

        @Override
        public PropertyAccessor createValueAccessor(Field field) {
            return new ClassFieldAccessor(object, field);
        }
    }

    private static class MapBackedPropertyAccessorFactory implements PropertyAccessorFactory {

        private final Map<String, Object> map;

        private MapBackedPropertyAccessorFactory(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public PropertyAccessor createValueAccessor(Field field) {
            return new MapEntryAccessor(map, field.getName());
        }
    }

    private static class ValueBackedPropertyAccessorFactory implements PropertyAccessorFactory {

        @Override
        public PropertyAccessor createValueAccessor(Field field) {
            return new DefaultPropertyAccessor();
        }
    }

}
