package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.*;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A registry for {@link Converter}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ConverterRegistry {
    private static final ConverterRegistry instance = new ConverterRegistry();
    private Map<Class<?>, Converter> converters;

    {
        converters = new HashMap<Class<?>, Converter>(10);

        // Primitive types
        converters.put(Boolean.TYPE, new BooleanConverter());
        converters.put(Character.TYPE, new CharacterConverter());
        converters.put(Byte.TYPE, new ByteConverter());
        converters.put(Short.TYPE, new ShortConverter());
        converters.put(Integer.TYPE, new IntegerConverter());
        converters.put(Long.TYPE, new LongConverter());
        converters.put(Float.TYPE, new FloatConverter());
        converters.put(Double.TYPE, new DoubleConverter());

        // Primitive type wrappers
        converters.put(Boolean.class, new BooleanConverter());
        converters.put(Character.class, new CharacterConverter());
        converters.put(Byte.class, new ByteConverter());
        converters.put(Short.class, new ShortConverter());
        converters.put(Integer.class, new IntegerConverter());
        converters.put(Long.class, new LongConverter());
        converters.put(Float.class, new FloatConverter());
        converters.put(Double.class, new DoubleConverter());

        // Objects
        converters.put(String.class, new StringConverter());
        converters.put(File.class, new FileConverter());
        converters.put(Date.class, new DateFormatConverter());
        converters.put(Pattern.class, new PatternConverter());
        converters.put(ValueRange.class, new IntervalConverter());
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
    public void setConverter(Class<?> type, Converter converter) {
        converters.put(type, converter);
    }

    /**
     * Gets the converter registered with the given type.
     *
     * @param type The type.
     * @return The converter or {@code null} if no such exists.
     */
    public Converter getConverter(Class<?> type) {
        Converter converter = converters.get(type);
        if (converter == null) {
            for (Map.Entry<Class<?>, Converter> entry : converters.entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    converter = entry.getValue();
                    break;
                }
            }
            if (converter == null) {
                if (type.isArray()) {
                    converter = getConverter(type.getComponentType());
                    if (converter != null) {
                        return new ArrayConverter(type, converter);
                    }
                } else if (type.isEnum()) {
                    return new EnumConverter((Class<? extends Enum>) type);
                }
            }
        }
        return converter;
    }
}
