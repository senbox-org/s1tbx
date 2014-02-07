package com.bc.ceres.binding;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
class ClassScanner {

    public static Map<String, Field> getFields(Class<?> type) {
        return getFields(type, FieldFilter.ALL);
    }

    public static Map<String, Field> getFields(Class<?> type, FieldFilter fieldFilter) {
        LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
        collectFields(type, fieldFilter, fields);
        return fields;
    }

    private static void collectFields(Class<?> type, FieldFilter fieldFilter, Map<String, Field> fields) {
        Class<?> superclass = type.getSuperclass();
        // Don't collect fields of java.lang.Object or pure interfaces
        if (superclass == null) {
            return;
        }
        // Collect superclass fields *before* collecting the type's fields
        collectFields(superclass, fieldFilter, fields);
        // Now collect the type's fields
        for (Field field : type.getDeclaredFields()) {
            if (fieldFilter.accept(field)) {
                fields.put(field.getName(), field);
            }
        }
    }

    public interface FieldFilter {
        FieldFilter ALL = new FieldFilter() {
            @Override
            public boolean accept(Field field) {
                return true;
            }
        };

        boolean accept(Field field);
    }
}
