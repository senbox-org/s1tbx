package com.bc.ceres.binding;

import com.bc.ceres.binding.validators.*;
import com.bc.ceres.binding.accessors.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

// todo - rename to ValueContainerFactory

/**
 * A factory for {@link ValueContainer}s
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueContainerFactory {
    public static final ValueDescriptorFactory DEFAULT_VALUE_DESCRIPTOR_FACTORY = new ValueDescriptorFactory() {
        public ValueDescriptor createValueDescriptor(Field field) {
            return new ValueDescriptor(field.getName(), field.getType());
        }
    };

    private final ValueDescriptorFactory valueDescriptorFactory;

    public ValueContainerFactory() {
        this(DEFAULT_VALUE_DESCRIPTOR_FACTORY);
    }

    public ValueContainerFactory(ValueDescriptorFactory valueDescriptorFactory) {
        this.valueDescriptorFactory = valueDescriptorFactory;
    }

    public ValueDescriptorFactory getValueDescriptorFactory() {
        return valueDescriptorFactory;
    }

    public ValueContainer createObjectBackedValueContainer(Object object) {
        Class<?> type = object.getClass();
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDescriptor valueDescriptor = createValueDescriptor(field);
            if (valueDescriptor != null) {
                vc.addModel(new ValueModel(valueDescriptor, new ClassFieldAccessor(object, field)));
            }
        }
        return vc;
    }

    public ValueContainer createValueBackedValueContainer(Class<?> type) {
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDescriptor valueDescriptor = createValueDescriptor(field);
            if (valueDescriptor != null) {
                vc.addModel(new ValueModel(valueDescriptor, new DefaultValueAccessor()));
            }
        }
        return vc;
    }

    public ValueContainer createMapBackedValueContainer(Class<?> type, Map<String, Object> map) {
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDescriptor valueDescriptor = createValueDescriptor(field);
            if (valueDescriptor != null) {
                vc.addModel(new ValueModel(valueDescriptor, new MapEntryAccessor(map, field.getName())));
            }
        }
        return vc;
    }

    public static ValueContainer createMapBackedValueContainer(Map<String, Object> map) {
        ValueContainer vc = new ValueContainer();
        for (Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            vc.addModel(new ValueModel(createValueDescriptor(name, value), new MapEntryAccessor(map, name)));
        }
        return vc;
    }

    private ValueDescriptor createValueDescriptor(Field field) {
        final ValueDescriptor valueDescriptor = valueDescriptorFactory.createValueDescriptor(field);
        if (valueDescriptor == null) {
            return null;
        }
        initValueDescriptor(valueDescriptor);
        return valueDescriptor;
    }

    private static ValueDescriptor createValueDescriptor(String name, Object value) {
        final ValueDescriptor valueDescriptor = new ValueDescriptor(name, value.getClass());
        valueDescriptor.setDefaultValue(value);
        initValueDescriptor(valueDescriptor);
        return valueDescriptor;
    }

    private static void initValueDescriptor(ValueDescriptor valueDescriptor) {
        if (valueDescriptor.getConverter() == null) {
            valueDescriptor.setDefaultConverter();
        }
        if (valueDescriptor.getValidator() == null) {
            valueDescriptor.setValidator(createValidator(valueDescriptor));
        }
    }

    private static Validator createValidator(ValueDescriptor vd) {
        List<Validator> validators = new ArrayList<Validator>(3);

        if (vd.isNotNull()) {
            validators.add(new NotNullValidator());
        }
        if (vd.isNotEmpty()) {
            validators.add(new NotEmptyValidator());
        }
        if (vd.getPattern() != null) {
            validators.add(new PatternValidator(vd.getPattern()));
        }
        if (vd.getValueSet() != null) {
            validators.add(new ValueSetValidator(vd.getValueSet()));
        }
        if (vd.getValueRange() != null) {
            validators.add(new IntervalValidator(vd.getValueRange()));
        }
        if (vd.getValidator() != null) {
            validators.add(vd.getValidator());
        }
        Validator validator;
        if (validators.isEmpty()) {
            validator = null;
        } else if (validators.size() == 1) {
            validator = validators.get(0);
        } else {
            validator = new MultiValidator(validators);
        }
        return validator;
    }
}
