package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MapParameterInjector implements ParameterInjector {

    private final Map<String, Object> parameters;

    public MapParameterInjector(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void injectParameters(Operator operator) throws OperatorException {
        Field[] parameterFields = getParameterFields(operator);
        for (Field parameterField : parameterFields) {
            Object value = parameters.get(parameterField.getName());
            if (value != null) {
                boolean oldFieldState = parameterField.isAccessible();
                try {
                    parameterField.setAccessible(true);
                    // todo - validate before setting the value!!!
                    parameterField.set(operator, value);
                } catch (IllegalAccessException e) {
                    throw new OperatorException("Failed to set parameter [" + parameterField.getName() + "]", e);
                } finally {
                    parameterField.setAccessible(oldFieldState);
                }
            }
        }
    }

    public static Field[] getParameterFields(Operator operator) {
        List<Field> parameterFields = new ArrayList<Field>();
        collectParameterFields(operator.getClass(), parameterFields);
        return parameterFields.toArray(new Field[parameterFields.size()]);
    }

    public static void collectParameterFields(Class operatorClass, List<Field> parameterFields) {
        final Class superclass = operatorClass.getSuperclass();
        if (superclass != null && superclass.isAssignableFrom(Operator.class)) {
            collectParameterFields(superclass, parameterFields);
        }
        Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(Parameter.class) != null) {
                parameterFields.add(declaredField);
            }
        }
    }
}
