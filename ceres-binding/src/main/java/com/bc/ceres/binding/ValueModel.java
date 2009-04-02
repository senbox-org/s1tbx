package com.bc.ceres.binding;

import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.accessors.MapEntryAccessor;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A model for a value, e.g. a field of an object instance. A value model is composed of a {@link ValueDescriptor} and
 * an {@link ValueAccessor}. Most of the time, value models are part of a {@link ValueContainer}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueModel {

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

    private final ValueDescriptor descriptor;

    private final ValueAccessor accessor;

    private ValueContainer container;

    public ValueModel(ValueDescriptor descriptor, ValueAccessor accessor) {
        this.descriptor = descriptor;
        this.accessor = accessor;
    }

    public static ValueModel createClassFieldModel(Object object, String name) {
        final Field field = getField(object, name);
        return createModel(createValueDescriptor(name, field.getType()), new ClassFieldAccessor(object, field));
    }

    public static ValueModel createClassFieldModel(Object object, String name, Object value) {
        final Field field = getField(object, name);
        return createModel(createValueDescriptor(name, field.getType()), new ClassFieldAccessor(object, field), value);
    }

    public static ValueModel createMapEntryModel(Map<String, Object> map, String name, Class<?> type) {
        return createModel(createValueDescriptor(name, type), new MapEntryAccessor(map, name), null);
    }

    public static ValueModel createMapEntryModel(Map<String, Object> map, String name, Class<?> type, Object value) {
        return createModel(createValueDescriptor(name, type), new MapEntryAccessor(map, name), value);
    }

    public static ValueModel createValueModel(String name, Class<?> type) {
        return createModel(createValueDescriptor(name, type), new DefaultValueAccessor(), null);
    }

    public static ValueModel createValueModel(String name, Object value) {
        return createModel(createValueDescriptor(name, value.getClass()), new DefaultValueAccessor(), value);
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
            return PRIMITIVE_ZERO_VALUES.get(descriptor.getType());
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
        return getClass().getName() + "[name=" + getDescriptor().getName() + ",value=" + getValueAsText() + "]";
    }

    private static ValueDescriptor createValueDescriptor(String name, Class<?> type) {
        return new ValueDescriptor(name, type);
    }

    private static ValueModel createModel(ValueDescriptor descriptor, ValueAccessor accessor) {
        ValueModel vm = new ValueModel(descriptor, accessor);
        vm.getDescriptor().setConverter(ConverterRegistry.getInstance().getConverter(descriptor.getType()));
        return vm;
    }

    private static ValueModel createModel(ValueDescriptor descriptor, ValueAccessor accessor, Object value) {
        ValueModel vm = createModel(descriptor, accessor);
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
