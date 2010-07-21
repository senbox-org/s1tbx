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

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A property is composed of a {@link PropertyDescriptor} (static type description) and
 * an {@link PropertyAccessor} (dynamic value assignment).
 * The {@link Property} interface is a realisation of the <i>Value Object</i> design pattern.
 * Most of the time, properties are used as part of a {@link PropertyContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class Property {

    static final HashMap<Class<?>, Object> PRIMITIVE_ZERO_VALUES;

    static {
        PRIMITIVE_ZERO_VALUES = new HashMap<Class<?>, Object>(17);
        PRIMITIVE_ZERO_VALUES.put(Boolean.TYPE, false);
        PRIMITIVE_ZERO_VALUES.put(Character.TYPE, (char) 0);
        PRIMITIVE_ZERO_VALUES.put(Byte.TYPE, (byte) 0);
        PRIMITIVE_ZERO_VALUES.put(Short.TYPE, (short) 0);
        PRIMITIVE_ZERO_VALUES.put(Integer.TYPE, 0);
        PRIMITIVE_ZERO_VALUES.put(Long.TYPE, (long) 0);
        PRIMITIVE_ZERO_VALUES.put(Float.TYPE, (float) 0);
        PRIMITIVE_ZERO_VALUES.put(Double.TYPE, (double) 0);
    }

    private final PropertyDescriptor descriptor;

    private final PropertyAccessor accessor;

    private PropertyContainer container;

    public Property(PropertyDescriptor descriptor, PropertyAccessor accessor) {
        this.descriptor = descriptor;
        this.accessor = accessor;
    }

    public static Property create(String name, Class<?> type) {
        return createImpl(createDescriptor(name, type), new DefaultPropertyAccessor(), null);
    }

    public static Property create(String name, Object value) {
        return createImpl(createDescriptor(name, value.getClass()), new DefaultPropertyAccessor(), value);
    }

    public static <T> Property create(String name, Class<? extends T> type, T defaultValue, boolean notNull) {
        final PropertyDescriptor descriptor = createDescriptor(name, type) ;
        if (notNull) {
            descriptor.setDefaultValue(defaultValue);
            descriptor.setNotNull(true);
        }
        return new Property(descriptor, new DefaultPropertyAccessor(defaultValue));
    }

    public static Property createForField(Object object, String name) {
        final Field field = getField(object, name);
        return createImpl(createDescriptor(name, field.getType()), new ClassFieldAccessor(object, field));
    }

    public static Property createForField(Object object, String name, Object value) {
        final Field field = getField(object, name);
        return createImpl(createDescriptor(name, field.getType()), new ClassFieldAccessor(object, field), value);
    }

    public static Property createForMapEntry(Map<String, Object> map, String name, Class<?> type) {
        return createImpl(createDescriptor(name, type), new MapEntryAccessor(map, name), null);
    }

    public static Property createForMapEntry(Map<String, Object> map, String name, Class<?> type, Object value) {
        return createImpl(createDescriptor(name, type), new MapEntryAccessor(map, name), value);
    }

    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    public PropertyContainer getContainer() {
        return container;
    }

    public void setContainer(PropertyContainer container) {
        this.container = container;
    }

    public Validator getValidator() {
        return descriptor.getEffectiveValidator();
    }

    public String getValueAsText() {
        final Converter converter = descriptor.getConverter(true);
        if (converter != null) {
            return converter.format(getValue());
        } else {
            return String.valueOf(getValue());
        }
    }

    public void setValueFromText(String text) throws ValidationException {
        final Converter converter = descriptor.getConverter(true);
        if (converter != null) {
            final Object value;
            try {
                value = converter.parse(text);
            } catch (ConversionException e) {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' is invalid.\n''{1}''",
                                                                   getDescriptor().getDisplayName(),
                                                                   e.getMessage()),
                                              e);
            }
            setValue(value);
        } else {
            setValue(text);
        }
    }

    public String getName() {
        return getDescriptor().getName();
    }

    public Class<?> getType() {
        return getDescriptor().getType();
    }

    public Object getValue() {
        final Object value = accessor.getValue();
        if (value == null && descriptor.getType().isPrimitive()) {
            return PRIMITIVE_ZERO_VALUES.get(descriptor.getType());
        }
        return value;
    }

    public void setValue(Object value) throws ValidationException {
        Object oldValue = getValue();
        if (equalObjects(oldValue, value)) {
            return;
        }
        validate(value);
        accessor.setValue(value);

        if (container != null) {
            container.getPropertyChangeSupport().firePropertyChange(descriptor.getName(), oldValue, value);
        }
    }

    private boolean equalObjects(Object oldValue, Object newValue) {
        if (oldValue == newValue) {
            return true;
        }
        if (oldValue == null) {
            return false;
        }
        if (oldValue instanceof Number && newValue instanceof Number) {
            final Number number = (Number) newValue;
            final Object epsValue = descriptor.getAttribute("eps");
            if (epsValue instanceof Number) {
                if ((oldValue instanceof Float)) {
                    final float v1 = (Float) oldValue;
                    final float v2 = number.floatValue();
                    final float deltaSig = v1 - v2;
                    final float deltaAbs = Math.abs(deltaSig);
                    final float eps = ((Number) epsValue).floatValue();
                    return deltaAbs < eps;
                } else if ((oldValue instanceof Double)) {
                    final double v1 = (Double) oldValue;
                    final double v2 = number.floatValue();
                    final double deltaSig = v1 - v2;
                    final double deltaAbs = Math.abs(deltaSig);
                    final double eps = ((Number) epsValue).floatValue();
                    return deltaAbs < eps;
                }
            }
        }
        return oldValue.equals(newValue);
    }

    public void validate(Object value) throws ValidationException {
        synchronized (this) {
            final Validator validator = getValidator();
            if (validator != null) {
                validator.validateValue(this, value);
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (container == null) {
            throw new IllegalStateException("container == null");
        }
        container.getPropertyChangeSupport().addPropertyChangeListener(descriptor.getName(), l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (container == null) {
            throw new IllegalStateException("container == null");
        }
        container.getPropertyChangeSupport().removePropertyChangeListener(descriptor.getName(), l);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",value=" + getValueAsText() + "]";
    }

    private static PropertyDescriptor createDescriptor(String name, Class<?> type) {
        return new PropertyDescriptor(name, type);
    }

    private static Property createImpl(PropertyDescriptor descriptor, PropertyAccessor accessor) {
        Property vm = new Property(descriptor, accessor);
        vm.getDescriptor().setConverter(ConverterRegistry.getInstance().getConverter(descriptor.getType()));
        return vm;
    }

    private static Property createImpl(PropertyDescriptor descriptor, PropertyAccessor accessor, Object value) {
        Property vm = createImpl(descriptor, accessor);
        if (value == null && descriptor.getType().isPrimitive()) {
            value = PRIMITIVE_ZERO_VALUES.get(descriptor.getType());
        }
        try {
            vm.setValue(value);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
        return vm;
    }

    private static Field getField(Object object, String name) {
        Field field;
        try {
            field = object.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        return field;
    }
}
