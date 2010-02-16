package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.PropertyAccessor;

import java.util.Map;

/**
 * ValueAccessor for values stored in a {@link Map}.
 */
public class MapEntryAccessor implements PropertyAccessor {
    private Map<String, Object> map;
    private String key;

    public MapEntryAccessor(Map<String, Object> map, String key) {
        this.map = map;
        this.key = key;
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
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }
}
