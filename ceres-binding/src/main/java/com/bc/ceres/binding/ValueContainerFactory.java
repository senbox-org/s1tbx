package com.bc.ceres.binding;

import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.validators.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


    /**
     * Creates a value container for the given object.
     * The factory method will not modify the object, thus not setting any default values.
     *
     * @param object the backing object
     * @return the value container
     */
    public ValueContainer createObjectBackedValueContainer(Object object) {
        return create(object.getClass(), new ObjectBackedValueAccessorFactory(object), false);
    }

    /**
     * Creates a value container for the given template type and map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param templateType the template type
     * @param map          the map which backs the values
     * @return the value container
     */
    public ValueContainer createMapBackedValueContainer(Class<?> templateType, Map<String, Object> map) {
        return create(templateType, new MapBackedValueAccessorFactory(map), false);
    }

    /**
     * Creates a value container for the given template type.
     * The value model returned will have its values set to defaults (if specified).
     *
     * @param templateType the template type
     * @return the value container
     */
    public ValueContainer createValueBackedValueContainer(Class<?> templateType) {
        return create(templateType, new ValueBackedValueAccessorFactory(), true);
    }

    public static ValueContainer createMapBackedValueContainer(Map<String, Object> map) {
        ValueContainer vc = new ValueContainer();
        for (Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            vc.addModel(new ValueModel(createValueDescriptor(name, value.getClass()),
                                       new MapEntryAccessor(map, name)));
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
        if (valueDescriptor.getDefaultValue() == null && valueDescriptor.getType().isPrimitive()) {
            valueDescriptor.setDefaultValue(ValueModel.PRIMITIVE_ZERO_VALUES.get(valueDescriptor.getType()));
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
            Validator valueSetValidator = new ValueSetValidator(vd);
            if (vd.getType().isArray()) {
                valueSetValidator = new ArrayValidator(valueSetValidator);
            }
            validators.add(valueSetValidator);
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

    private ValueContainer create(Class<?> type, ValueAccessorFactory valueAccessorFactory, boolean setDefaultValues) {
        ValueContainer vc = new ValueContainer();
        collect(vc, type, valueAccessorFactory);
        if (setDefaultValues) {
            try {
                vc.setDefaultValues();
            } catch (ValidationException e) {
                throw new IllegalStateException(e);
            }
        }
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

    private interface ValueAccessorFactory {
        ValueAccessor create(Field field);
    }

    private class ObjectBackedValueAccessorFactory implements ValueAccessorFactory {
        private Object object;

        private ObjectBackedValueAccessorFactory(Object object) {
            this.object = object;
        }

        public ValueAccessor create(Field field) {
            return new ClassFieldAccessor(object, field);
        }
    }

    private class MapBackedValueAccessorFactory implements ValueAccessorFactory {
        private Map<String, Object> map;

        private MapBackedValueAccessorFactory(Map<String, Object> map) {
            this.map = map;
        }

        public ValueAccessor create(Field field) {
            return new MapEntryAccessor(map, field.getName());
        }
    }

    private class ValueBackedValueAccessorFactory implements ValueAccessorFactory {

        public ValueAccessor create(Field field) {
            return new DefaultValueAccessor();
        }
    }
}
