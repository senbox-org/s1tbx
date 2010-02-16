package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class ClassConverter implements Converter<Class> {
    private final ClassLoader classLoader;
    private static Map<String, Class<?>> primitiveTypes;

    static {
        primitiveTypes = new HashMap<String, Class<?>>();
        primitiveTypes.put("char", Character.TYPE);
        primitiveTypes.put("boolean", Boolean.TYPE);
        primitiveTypes.put("byte", Byte.TYPE);
        primitiveTypes.put("short", Short.TYPE);
        primitiveTypes.put("int", Integer.TYPE);
        primitiveTypes.put("long", Long.TYPE);
        primitiveTypes.put("float", Float.TYPE);
        primitiveTypes.put("double", Double.TYPE);
        primitiveTypes.put("void", Void.TYPE);
    }

    public ClassConverter() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClassConverter(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    @Override
    public Class<? extends Class> getValueType() {
        return Class.class;
    }

    /**
     * Converts a value from its plain text representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param text The textual representation of the value.
     * @return The converted value.
     * @throws com.bc.ceres.binding.ConversionException
     *          If the conversion fails.
     */
    @Override
    public Class parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        Class<?> aClass = primitiveTypes.get(text);
        if (aClass != null) {
            return aClass;
        }
        if (text.indexOf('.') == -1) {
            try {
                return Class.forName("java.lang." + text);
            } catch (ClassNotFoundException e) {
                // ok
            }
        }
        try {
            return classLoader.loadClass(text);
        } catch (ClassNotFoundException e) {
            throw new ConversionException(MessageFormat.format("''{0}'' is not a known type.", text));
        }
    }

    /**
     * Converts a value of the type returned by {@link #getValueType()} to its
     * plain text representation.
     *
     * @param value The value to be converted to text.
     * @return The textual representation of the value.
     */
    @Override
    public String format(Class value) {
        if (value == null) {
            return "";
        }
        Package aPackage = value.getPackage();
        if (aPackage == null || aPackage.getName().equals("java.lang")) {
            return value.getSimpleName();
        }
        return value.getName();
    }
}
