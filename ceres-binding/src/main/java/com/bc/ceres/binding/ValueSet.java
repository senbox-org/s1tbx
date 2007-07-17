package com.bc.ceres.binding;

import java.util.List;
import java.util.Arrays;
import java.util.StringTokenizer;

public class ValueSet {

    private List<Object> objects;

    public ValueSet(Object[] items) {
        this.objects = Arrays.asList(items);
    }

    public Object[] getItems() {
        return objects.toArray();
    }

    public boolean contains(Object value) {
        return objects.contains(value);
    }

    public static ValueSet parseValueSet(String valueStrings, String separators, Converter converter) throws ConversionException {
        StringTokenizer st = new StringTokenizer(valueStrings, separators);
        Object[] objects = new Object[st.countTokens()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = converter.parse(st.nextToken().trim());
        }
        return new ValueSet(objects);
    }


    public static ValueSet parseValueSet(String[] valueStrings, Converter converter) throws ConversionException {
        Object[] objects = new Object[valueStrings.length];
        for (int i = 0; i < valueStrings.length; i++) {
            objects[i] = converter.parse(valueStrings[i]);
        }
        return new ValueSet(objects);
    }
}
