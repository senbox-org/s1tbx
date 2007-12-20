package com.bc.ceres.binding;

import java.beans.PropertyChangeListener;

/**
 * A model for a value, e.g. a field of an object instance. A value model is composed of a {@link ValueDescriptor} and
 * an {@link ValueAccessor}. Most of the time, value models are part of a {@link ValueContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueModel {

    private final ValueDescriptor descriptor;
    private final ValueAccessor accessor;
    private ValueContainer container;

    public ValueModel(ValueDescriptor descriptor, ValueAccessor accessor) {
        this.descriptor = descriptor;
        this.accessor = accessor;
        try {
            initializeValue();
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
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
        return accessor.getValue();
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


    private void initializeValue() throws ValidationException {
        // todo - use descriptor.isDefaultValueSet() to check, if true, always set any default value incl. null
        if (descriptor.getDefaultValue() != null) {
            setValue(descriptor.getDefaultValue());
        } else {
            // primitive types shall have initial values in all kinds of value model
            final Class<?> type = descriptor.getType();
            if (getValue() == null && type.isPrimitive()) {
                if (type.equals(Boolean.TYPE)) {
                    setValue(false);
                } else if (type.equals(Character.TYPE)) {
                    setValue((char) 0);
                } else if (type.equals(Byte.TYPE)) {
                    setValue((byte) 0);
                } else if (type.equals(Short.TYPE)) {
                    setValue((short) 0);
                } else if (type.equals(Integer.TYPE)) {
                    setValue(0);
                } else if (type.equals(Long.TYPE)) {
                    setValue((long) 0);
                } else if (type.equals(Float.TYPE)) {
                    setValue((float) 0);
                } else if (type.equals(Double.TYPE)) {
                    setValue((double) 0);
                } else {
                    // todo - warn: new primitive Java type since 1.6?
                }
            }
        }
    }
}
