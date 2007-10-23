package com.bc.ceres.binding;

import com.bc.ceres.binding.validators.*;
import com.bc.ceres.binding.accessors.*;

import java.lang.reflect.Field;
import java.util.*;

// todo - rename to ValueContainerFactory

/**
 * A factory for {@link ValueContainer}s
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueContainerFactory {
    public static final ValueDefinitionFactory DEFAULT_VALUE_DEFINITION_FACTORY = new ValueDefinitionFactory() {
        public ValueDefinition createValueDefinition(Field field) {
            return new ValueDefinition(field.getName(), field.getType());
        }
    };

    private final ValueDefinitionFactory valueDefinitionFactory;

    public ValueContainerFactory() {
        this(DEFAULT_VALUE_DEFINITION_FACTORY);
    }

    public ValueContainerFactory(ValueDefinitionFactory valueDefinitionFactory) {
        this.valueDefinitionFactory = valueDefinitionFactory;
    }

    public ValueDefinitionFactory getValueDefinitionFactory() {
        return valueDefinitionFactory;
    }

    public ValueContainer createObjectBackedValueContainer(Object object) {
        Class<?> type = object.getClass();
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDefinition valueDefinition = createValueDefinition(field);
            if (valueDefinition != null) {
                vc.addModel(new ValueModel(valueDefinition, new ClassFieldAccessor(object, field)));
            }
        }
        return vc;
    }

    public ValueContainer createValueBackedValueContainer(Class<?> type) {
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDefinition valueDefinition = createValueDefinition(field);
            if (valueDefinition != null) {
                vc.addModel(new ValueModel(valueDefinition, new DefaultAccessor()));
            }
        }
        return vc;
    }

    public ValueContainer createMapBackedValueContainer(Class<?> type, Map<String, Object> map) {
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDefinition valueDefinition = createValueDefinition(field);
            if (valueDefinition != null) {
                vc.addModel(new ValueModel(valueDefinition, new MapEntryAccessor(map, field.getName())));
            }
        }
        return vc;
    }

    public static ValueContainer createMapBackedValueContainer(Map<String, Object> map) {
        ValueContainer vc = new ValueContainer();
        for (String name : map.keySet()) {
            vc.addModel(new ValueModel(createValueDefinition(name, map.get(name)), new MapEntryAccessor(map, name)));
        }
        return vc;
    }

    private ValueDefinition createValueDefinition(Field field) {
        final ValueDefinition valueDefinition = valueDefinitionFactory.createValueDefinition(field);
        if (valueDefinition == null) {
            return null;
        }
        initValueDefinition(valueDefinition);
        return valueDefinition;
    }

    private static ValueDefinition createValueDefinition(String name, Object value) {
        final ValueDefinition valueDefinition = new ValueDefinition(name, value.getClass());
        valueDefinition.setDefaultValue(value);
        initValueDefinition(valueDefinition);
        return valueDefinition;
    }

    private static void initValueDefinition(ValueDefinition valueDefinition) {
        if (valueDefinition.getConverter() == null) {
            valueDefinition.setConverter(ConverterRegistry.getInstance().getConverter(valueDefinition.getType()));
        }
        if (valueDefinition.getValidator() == null) {
            valueDefinition.setValidator(createValidator(valueDefinition));
        }
    }

    private static Validator createValidator(ValueDefinition vd) {
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
        if (vd.getInterval() != null) {
            validators.add(new IntervalValidator(vd.getInterval()));
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
