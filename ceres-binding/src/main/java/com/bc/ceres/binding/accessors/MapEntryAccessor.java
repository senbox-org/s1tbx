/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
