package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.ValueAccessor;

/**
 * A base class for value accessor which know their value type.
 */
public abstract class TypesafeValueAccessor implements ValueAccessor {
    private final Class<?> valueType;

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     * @param valueType the type of the value
     */
    protected TypesafeValueAccessor(Class<?> valueType) {
        this.valueType = valueType;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    protected void checkValue(Object value) {
        // todo
    }

    protected Object getInitialValue() {
        if (valueType.isPrimitive()) {
            if (valueType.equals(Boolean.TYPE)) {
                return false;
            } else if (valueType.equals(Character.TYPE)) {
                return (char) 0;
            } else if (valueType.equals(Byte.TYPE)) {
                return (byte) 0;
            } else if (valueType.equals(Short.TYPE)) {
                return (short) 0;
            } else if (valueType.equals(Integer.TYPE)) {
                return 0;
            } else if (valueType.equals(Long.TYPE)) {
                return (long) 0;
            } else if (valueType.equals(Float.TYPE)) {
                return (float) 0;
            } else if (valueType.equals(Double.TYPE)) {
                return (double) 0;
            }
        }
        return null;
    }
}