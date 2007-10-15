package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * A factory for value definitions derived from object {@link Field}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface ValueDefinitionFactory {
    /**
     * Creates a new value definition for the given field.
     *
     * @param field The field.
     * @return The value definition.
     */
    ValueDefinition createValueDefinition(Field field);
}
