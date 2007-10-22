package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.lang.reflect.Array;
import java.util.StringTokenizer;

public class ArrayConverter implements Converter {

    private static final String COMMA_REPLACEMENT = createCommaReplacement();
    private Class arrayType;
    private Converter componentConverter;

    public ArrayConverter(Class arrayType, Converter componentConverter) {
        this.arrayType = arrayType;
        this.componentConverter = componentConverter;
    }

    public Class<?> getValueType() {
        return arrayType;
    }

    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(text, ",");
        int length = st.countTokens();
        Object array = Array.newInstance(arrayType.getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object component = componentConverter.parse(st.nextToken().replace(COMMA_REPLACEMENT, ","));
            Array.set(array, i, component);
        }
        return array;
    }

    public String format(Object array) {
        if (array == null) {
            return "";
        }
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder(length * 4);
        for (int i = 0; i < length; i++) {
            Object component = Array.get(array, i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(componentConverter.format(component).replace(",", COMMA_REPLACEMENT));
        }
        return sb.toString();
    }

    private static String createCommaReplacement() {
        String s = Integer.toOctalString(',');
        while (s.length() < 4) {
            s = "0" + s;
        }
        return "\\"+ s;
    }
}