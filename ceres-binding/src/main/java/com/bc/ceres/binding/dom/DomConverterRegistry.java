package com.bc.ceres.binding.dom;

import java.util.HashMap;
import java.util.Map;

public class DomConverterRegistry {

    private static final DomConverterRegistry INSTANCE = new DomConverterRegistry();
    private Map<Class<?>, DomConverter> converterMap = new HashMap<Class<?>, DomConverter>(33);

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static DomConverterRegistry getInstance() {
        return DomConverterRegistry.INSTANCE;
    }

    /**
     * Sets the converter to be used for the specified type.
     *
     * @param type      The type.
     * @param converter The converter.
     */
    public void setDomConverter(Class<?> type, DomConverter converter) {
        converterMap.put(type, converter);
    }

    /**
     * Gets the converter registered with the given type.
     *
     * @param type The type.
     *
     * @return The converter for the given type or {@code null} if no such converter exists.
     */
    public DomConverter getConverter(Class<?> type) {
        DomConverter domConverter = converterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = converterMap.get(type);
        }
        return domConverter;
    }
}
