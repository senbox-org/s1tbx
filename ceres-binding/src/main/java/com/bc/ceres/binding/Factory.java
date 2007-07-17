package com.bc.ceres.binding;

import com.bc.ceres.binding.validators.*;
import com.bc.ceres.binding.accessors.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class Factory {
    private final ValueDefinitionFactory valueDefinitionFactory;

    public Factory(ValueDefinitionFactory valueDefinitionFactory) {
        this.valueDefinitionFactory = valueDefinitionFactory;
    }

    public ValueDefinitionFactory getPropertyDefinitionFactory() {
        return valueDefinitionFactory;
    }

    public ValueContainer createObjectBackedValueContainer(Object object) {
        Class<?> type = object.getClass();
        Field[] declaredFields = type.getDeclaredFields();
        ValueContainer vc = new ValueContainer();
        for (Field field : declaredFields) {
            final ValueDefinition valueDefinition = createPropertyDefinition(field);
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
            final ValueDefinition valueDefinition = createPropertyDefinition(field);
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
            final ValueDefinition valueDefinition = createPropertyDefinition(field);
            if (valueDefinition != null) {
                vc.addModel(new ValueModel(valueDefinition, new MapEntryAccessor(map, field.getName())));
            }
        }
        return vc;
    }

    private ValueDefinition createPropertyDefinition(Field field) {
        final ValueDefinition valueDefinition = valueDefinitionFactory.createValueDefinition(field);
        if (valueDefinition == null) {
            return null;
        }
        if (valueDefinition.getConverter() == null) {
            valueDefinition.setConverter(ConverterRegistry.getInstance().getConverter(valueDefinition.getType()));
        }
        if (valueDefinition.getValidator() == null) {
            valueDefinition.setValidator(createValidator(valueDefinition));
        }
        return valueDefinition;
    }

    private Validator createValidator(ValueDefinition vd)  {
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
