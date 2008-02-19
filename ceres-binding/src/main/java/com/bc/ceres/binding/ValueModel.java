package com.bc.ceres.binding;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

/**
 * A model for a value, e.g. a field of an object instance. A value model is composed of a {@link ValueDescriptor} and
 * an {@link ValueAccessor}. Most of the time, value models are part of a {@link ValueContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueModel {

    static final HashMap<Class<?>, Object> INITIAL_VALUES;

    static  {
        INITIAL_VALUES = new HashMap<Class<?>, Object>(17);
        INITIAL_VALUES.put(Boolean.TYPE, Boolean.FALSE);
        INITIAL_VALUES.put(Character.TYPE, (char) 0);
        INITIAL_VALUES.put(Byte.TYPE, (byte) 0);
        INITIAL_VALUES.put(Short.TYPE, (short) 0);
        INITIAL_VALUES.put(Integer.TYPE, 0);
        INITIAL_VALUES.put(Long.TYPE, (long) 0);
        INITIAL_VALUES.put(Float.TYPE, (float) 0);
        INITIAL_VALUES.put(Double.TYPE, (double) 0);
    }

    private final ValueDescriptor descriptor;

    private final ValueAccessor accessor;

    private ValueContainer container;

    public ValueModel(ValueDescriptor descriptor, ValueAccessor accessor) {
        this.descriptor = descriptor;
        this.accessor = accessor;
    }

    public ValueDescriptor getDescriptor() {
        return descriptor;
    }

    public ValueContainer getContainer() {
        return container;
    }

    public void setContainer(ValueContainer container) {
        this.container = container;
    }

    public String getValueAsText() {
        final Converter converter = descriptor.getConverter(true);
        if (converter != null) {
            return converter.format(getValue());
        } else {
            return String.valueOf(getValue());
        }
    }

    public void setValueFromText(String text) throws ConversionException, ValidationException {
        final Converter converter = descriptor.getConverter(true);
        if (converter != null) {
            setValue(converter.parse(text));
        } else {
            setValue(text);
        }
    }

    public Object getValue() {
        final Object value = accessor.getValue();
        if (value == null && descriptor.getType().isPrimitive()) {
            return INITIAL_VALUES.get(descriptor.getType());
        }
        return value;
    }

    public void setValue(Object value) throws ValidationException {
        Object oldValue = getValue();
        if (oldValue == value || oldValue != null && oldValue.equals(value)) {
            return;
        }
        validate(value);
        accessor.setValue(value);

        if (container != null) {
            container.getPropertyChangeSupport().firePropertyChange(descriptor.getName(), oldValue, value);
        }
    }

    public void validate(Object value) throws ValidationException {
        final Validator validator = descriptor.getValidator();
        if (validator != null) {
            validator.validateValue(this, value);
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

    private Object getPrimitiveInitialValue(Class<?> valueType) {
        return INITIAL_VALUES.get(valueType);
    }
}
