package com.bc.ceres.binding;

import java.lang.reflect.Field;

public interface ValueDefinitionFactory {
    ValueDefinition createValueDefinition(Field field);
}
