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
package org.esa.snap.core.util.geotiff;

import java.lang.reflect.Field;
import java.util.HashMap;

public class IntMap {

    private HashMap<String, Integer> _valueMap;
    private HashMap<Integer, String> _nameMap;

    protected void init(Field[] fields) {
        _valueMap = new HashMap<String, Integer>();
        _nameMap = new HashMap<Integer, String>();
        for (Field field : fields) {
            try {
                final String name = field.getName();
                final Integer value = (Integer) field.get(null);
                _valueMap.put(name, value);
                _nameMap.put(value, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getCode(String name) {
        return _valueMap.get(name);
    }

    public String getName(int code) {
        return _nameMap.get(Integer.valueOf(code));
    }

    protected IntMap() {
    }
}

