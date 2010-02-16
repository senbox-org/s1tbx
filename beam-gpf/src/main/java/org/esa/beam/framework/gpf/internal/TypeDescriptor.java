package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TypeDescriptor {

    private final Class<?> type;
    private Map<Field, Parameter> parameterMap;

    public TypeDescriptor(Class<?> type) {
        this.type = type;
        parameterMap = new HashMap<Field, Parameter>();
        processAnnotationsRec(type);
    }

    public final Class<?> getType() {
        return type;
    }

    public final Map<Field, Parameter> getParameterMap() {
        return parameterMap;
    }

    private void processAnnotationsRec(Class<?> type) {
        final Class<?> superclass = type.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            processAnnotationsRec(superclass);
        }
        final Field[] declaredFields = type.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            final Parameter parameter = declaredField.getAnnotation(Parameter.class);
            if (parameter != null) {
                parameterMap.put(declaredField, parameter);
            }
        }
    }
}