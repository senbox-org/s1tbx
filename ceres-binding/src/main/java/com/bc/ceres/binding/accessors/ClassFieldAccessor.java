package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.Accessor;

import java.lang.reflect.Field;


public class ClassFieldAccessor implements Accessor {
    private Object object;
    private Field field;

    public ClassFieldAccessor(Object instance, Field field) {
        this.object = instance;
        this.field = field;
    }

    public Object getValue() {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }

    public void setValue(Object value) {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }
}
