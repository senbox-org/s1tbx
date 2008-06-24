package com.bc.ceres.binding;

import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.validators.TypeValidator;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

/**
 * A model for a value, e.g. a field of an object instance. A value model is composed of a {@link ValueDescriptor} and
 * an {@link ValueAccessor}. Most of the time, value models are part of a {@link ValueContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueModel {

    static final HashMap<Class<?>, Object> INITIAL_VALUES;

    static {
        INITIAL_VALUES = new HashMap<Class<?>, Object>(17);
        INITIAL_VALUES.put(Boolean.TYPE, false);
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

    public static ValueModel create(Object object, String name) {
        Field field;
        try {
            field = object.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        ValueModel vm = new ValueModel(new ValueDescriptor(name, field.getType()),
                                       new ClassFieldAccessor(object, field));
        vm.getDescriptor().setValidator(new TypeValidator());
        return vm;
    }

    public static ValueModel create(Map<String, Object> map, String name, Class<?> type) {
        ValueModel vm = new ValueModel(new ValueDescriptor(name, type),
                                       new MapEntryAccessor(map, name));
        vm.getDescriptor().setValidator(new TypeValidator());
        initPrimitiveValue(type, vm);
        return vm;
    }

    public static ValueModel create(String name, Class<?> type) {
        ValueModel vm = new ValueModel(new ValueDescriptor(name, type),
                                       new DefaultValueAccessor());
        vm.getDescriptor().setValidator(new TypeValidator());
        initPrimitiveValue(type, vm);
        return vm;
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

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getDescriptor().getName() + ",value=" + getValueAsText() + "]";
    }

    private static void initPrimitiveValue(Class<?> type, ValueModel vm) {
        if (type.isPrimitive()) {
            try {
                vm.setValue(getPrimitiveInitialValue(type));
            } catch (ValidationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static Object getPrimitiveInitialValue(Class<?> valueType) {
        return INITIAL_VALUES.get(valueType);
    }
}
