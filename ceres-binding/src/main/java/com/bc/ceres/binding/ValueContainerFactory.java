package com.bc.ceres.binding;

import com.bc.ceres.binding.validators.*;
import com.bc.ceres.binding.accessors.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;


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

    private interface ValueAccessorFactory {
        ValueAccessor create(Field field);
    }

    private class ObjectBackedValueAccessorFactory implements ValueAccessorFactory{
        private Object object;

        private ObjectBackedValueAccessorFactory(Object object) {
            this.object = object;
        }

        public ValueAccessor create(Field field) {
            return new ClassFieldAccessor(object, field);
        }
    }

    private class MapBackedValueAccessorFactory implements ValueAccessorFactory{
        private Map<String, Object> map;

        private MapBackedValueAccessorFactory(Map<String, Object> map) {
            this.map = map;
        }

        public ValueAccessor create(Field field) {
            return new  MapEntryAccessor(map, field.getName(), field.getType());
        }
    }

    private class ValueBackedValueAccessorFactory implements ValueAccessorFactory{

        public ValueAccessor create(Field field) {
            return new DefaultValueAccessor(field.getType());
        }
    }

    public ValueContainer createObjectBackedValueContainer(Object wrappedObject) {
        return x(wrappedObject.getClass(), new ObjectBackedValueAccessorFactory(wrappedObject));
    }

    public ValueContainer createValueBackedValueContainer(Class<?> templateType) {
        return x(templateType, new ValueBackedValueAccessorFactory());
    }


    public ValueContainer createMapBackedValueContainer(Class<?> templateType, Map<String, Object> map) {
        return x(templateType, new MapBackedValueAccessorFactory(map));
    }

    public static ValueContainer createMapBackedValueContainer(Map<String, Object> map) {
        ValueContainer vc = new ValueContainer();
        for (Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            vc.addModel(new ValueModel(createValueDescriptor(name, value.getClass()),
                                       new MapEntryAccessor(map, name, value.getClass())));
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

    private static ValueDescriptor createValueDescriptor(String name, Class<? extends Object> type) {
        final ValueDescriptor valueDescriptor = new ValueDescriptor(name, type);
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

        validators.add(new TypeValidator());

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

    private ValueContainer x(Class<?> type, ValueAccessorFactory valueAccessorFactory) {
        ValueContainer vc = new ValueContainer();
        collect(vc, type, valueAccessorFactory);
        return vc;
    }

    private void collect(ValueContainer vc, Class<?> type, ValueAccessorFactory valueAccessorFactory) {
        if (!type.equals(Object.class)) {
            collect(vc, type.getSuperclass(), valueAccessorFactory);
            Field[] declaredFields = type.getDeclaredFields();
            for (Field field : declaredFields) {
                final ValueDescriptor valueDescriptor = createValueDescriptor(field);
                if (valueDescriptor != null) {
                    vc.addModel(new ValueModel(valueDescriptor, valueAccessorFactory.create(field)));
                }
            }
        }
    }
}
