package com.bc.ceres.binding;

import com.bc.ceres.binding.converters.AffineTransformConverter;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.BooleanConverter;
import com.bc.ceres.binding.converters.ByteConverter;
import com.bc.ceres.binding.converters.CharacterConverter;
import com.bc.ceres.binding.converters.ColorConverter;
import com.bc.ceres.binding.converters.DateFormatConverter;
import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.converters.EnumConverter;
import com.bc.ceres.binding.converters.FileConverter;
import com.bc.ceres.binding.converters.FloatConverter;
import com.bc.ceres.binding.converters.FontConverter;
import com.bc.ceres.binding.converters.IntegerConverter;
import com.bc.ceres.binding.converters.IntervalConverter;
import com.bc.ceres.binding.converters.LongConverter;
import com.bc.ceres.binding.converters.PatternConverter;
import com.bc.ceres.binding.converters.ShortConverter;
import com.bc.ceres.binding.converters.StringConverter;
import com.bc.ceres.binding.converters.UrlConverter;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.net.URL;
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

    private Map<Class<?>, Converter<?>> converters;

    private ConverterRegistry() {
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
        setConverter(AffineTransform.class, new AffineTransformConverter());
        setConverter(Color.class, new ColorConverter());
        setConverter(Date.class, new DateFormatConverter());
        setConverter(File.class, new FileConverter());
        setConverter(URL.class, new UrlConverter());
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
        return Holder.instance;
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
     *
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
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final ConverterRegistry instance = new ConverterRegistry();
    }
}
