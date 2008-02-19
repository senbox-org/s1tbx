package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.ValueAccessor;

import java.util.Map;

/**
 * ValueAccessor for values stored in a {@link Map}.
 */
public class MapEntryAccessor extends TypesafeValueAccessor {
    private Map<String, Object> map;
    private String key;

    public MapEntryAccessor(Map<String, Object> map, String key, Class<?> type) {
        super(type);
        this.map = map;
        this.key = key;
        if (map.get(key) == null) {
            setValue(getInitialValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() {
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Object value) {
        checkValue(value);
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }
}
