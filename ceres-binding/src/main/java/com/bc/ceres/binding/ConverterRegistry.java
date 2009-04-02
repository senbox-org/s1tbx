package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.*;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Font;

/**
 * A registry for {@link Converter}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ConverterRegistry {
    private static final ConverterRegistry instance = new ConverterRegistry();
    private Map<Class<?>, Converter<?>> converters;

    {
        converters = new HashMap<Class<?>, Converter<?>>(33);

        // Primitive types
        setConverter(Boolean.TYPE, new BooleanConverter());
        setConverter(Character.TYPE, new CharacterConverter());
        setConverter(Byte.TYPE, new ByteConverter());
        setConverter(Short.TYPE, new ShortConverter());
        setConverter(Integer.TYPE, new IntegerConverter());
        setConverter(Long.TYPE, new LongConverter());
        setConverter(Float.TYPE, new FloatConverter());
        setConverter(Double.TYPE, new DoubleConverter());

        // Primitive type wrappers
        setConverter(Boolean.class, new BooleanConverter());
        setConverter(Character.class, new CharacterConverter());
        setConverter(Byte.class, new ByteConverter());
        setConverter(Short.class, new ShortConverter());
        setConverter(Integer.class, new IntegerConverter());
        setConverter(Long.class, new LongConverter());
        setConverter(Float.class, new FloatConverter());
        setConverter(Double.class, new DoubleConverter());

        // Objects
        setConverter(Color.class, new ColorConverter());
        setConverter(Date.class, new DateFormatConverter());
        setConverter(File.class, new FileConverter());
        setConverter(Font.class, new FontConverter());
        setConverter(Pattern.class, new PatternConverter());
        setConverter(String.class, new StringConverter());
        setConverter(ValueRange.class, new IntervalConverter());
    }

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static ConverterRegistry getInstance() {
        return ConverterRegistry.instance;
    }

    /**
     * Sets the converter to be used for the specified type.
     *
     * @param type      The type.
     * @param converter The converter.
     */
    public <T> void setConverter(Class<? extends T> type, Converter<T> converter) {
        converters.put(type, converter);
    }

    /**
     * Gets the converter registered with the given type.
     *
     * @param type The type.
     * @return The converter or {@code null} if no such exists.
     */
    public <T> Converter<T> getConverter(Class<? extends T> type) {
        Converter<?> converter = converters.get(type);
        if (converter == null) {
            for (Map.Entry<Class<?>, Converter<?>> entry : converters.entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    converter = entry.getValue();
                    break;
                }
            }
            if (converter == null) {
                if (type.isArray()) {
                    converter = getConverter(type.getComponentType());
                    if (converter != null) {
                        return (Converter<T>) new ArrayConverter(type, converter);
                    }
                } else if (type.isEnum()) {
                    return new EnumConverter(type);
                }
            }
        }
        return (Converter<T>) converter;
    }
}
