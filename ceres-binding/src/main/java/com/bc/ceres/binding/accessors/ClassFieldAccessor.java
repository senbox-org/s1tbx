package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.PropertyAccessor;

import java.lang.reflect.Field;

/**
 * ValueAccessor for values stored in a {@link Field} of a plain Java object.
 */
public class ClassFieldAccessor implements PropertyAccessor {
    private Object object;
    private Field field;

    /**
     * Constructs a new {@code ClassFieldAccessor}.
     *
     * @param instance the plain Java object
     * @param field    the field which holds the value
     */
    public ClassFieldAccessor(Object instance, Field field) {
        this.object = instance;
        this.field = field;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
