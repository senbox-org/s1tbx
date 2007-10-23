package com.bc.ceres.binding;

import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeListener;

/**
 * A model for a value, e.g. a field of an object instance. A value model is composed of a {@link ValueDefinition} and
 * an {@link Accessor}. Most of the time, value models are part of a {@link ValueContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueModel {
    private final ValueDefinition valueDefinition;
    private final Accessor accessor;
    private ValueContainer container;

    public ValueModel(ValueDefinition valueDefinition, Accessor accessor) {
        this.valueDefinition = valueDefinition;
        this.accessor = accessor;
        try {
            initializeValue();
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ValueDefinition getDefinition() {
        return valueDefinition;
    }

    public ValueContainer getContainer() {
        return container;
    }

    public void setContainer(ValueContainer container) {
        this.container = container;
    }

//    public Converter getConverter() {
//        return getDefinition().getConverter();
//    }
//
//    public String getName() {
//        return getDefinition().getName();
//    }
//
//    public Class<?> getType() {
//        return getDefinition().getType();
//    }
//
//    public Validator getValidator() {
//        return getDefinition().getValidator();
//    }

    public String getAsText() {
        final Converter converter = valueDefinition.getConverter(true);
        if (converter != null) {
            return converter.format(getValue());
        } else {
            return String.valueOf(getValue());
        }
    }

    public void setFromText(String text) throws ConversionException, ValidationException {
        final Converter converter = valueDefinition.getConverter(true);
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
            container.getPropertyChangeSupport().firePropertyChange(valueDefinition.getName(), oldValue, value);
        }
    }

    public void validate(Object value) throws ValidationException {
        final Validator validator = valueDefinition.getValidator();
        if (validator != null) {
            validator.validateValue(this, value);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (container == null) {
            throw new IllegalStateException("container == null");
        }
        container.getPropertyChangeSupport().addPropertyChangeListener(valueDefinition.getName(), l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (container == null) {
            throw new IllegalStateException("container == null");
        }
        container.getPropertyChangeSupport().removePropertyChangeListener(valueDefinition.getName(), l);
    }


    public void initializeValue() throws ValidationException {
        if (getValue() != null) {
            return;
        }
        if (valueDefinition.getDefaultValue() != null) {
            setValue(valueDefinition.getDefaultValue());
        }
        if (getValue() != null) {
            return;
        }
        final Class<?> type = valueDefinition.getType();
        if (type.isPrimitive()) {
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
            }
        }
    }
}
