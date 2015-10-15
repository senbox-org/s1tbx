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

package org.esa.snap.core.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class IntMap implements Cloneable {
    public static final int NULL = Integer.MIN_VALUE;
    private final Map<Integer, Integer> map;
    private final int[] table;
    private final int tableOffset;
    private int size;

    public IntMap() {
        this(0, 1024);
    }

    public IntMap(int tableOffset, int bufferSize) {
        map = new TreeMap<Integer, Integer>();
        table = new int[bufferSize];
        this.tableOffset = tableOffset;
        this.size = 0;
        Arrays.fill(table, NULL);
    }

    public int getSize() {
        return size;
    }

    public void putValue(int key, int value) {
        if (value == NULL) {
            throw new IllegalArgumentException("value");
        }
        final int index = key - tableOffset;
        if (index >= 0 && index < table.length) {
            final int oldValue = table[index];
            table[index] = value;
            if (oldValue == NULL) {
                size++;
            }
        } else {
            final Integer oldValue = map.put(key, value);
            if (oldValue == null) {
                size++;
            }
        }
    }

    public void removeValue(int key) {
        final int index = key - tableOffset;
        if (index >= 0 && index < table.length) {
            final int oldValue = table[index];
            table[index] = NULL;
            if (oldValue != NULL) {
                size--;
            }
        } else {
            final Integer oldValue = map.remove(key);
            if (oldValue != null) {
                size--;
            }
        }
    }

    public int getValue(int key) {
        final int index = key - tableOffset;
        if (index >= 0 && index < table.length) {
            return table[index];
        } else {
            final Integer oldValue = map.get(key);
            if (oldValue != null) {
                return oldValue;
            } else {
                return NULL;
            }
        }
    }

    /**
     * Gets the array of keys in this map, sorted in ascending order.
     * @return the array of keys in the map
     */
    public int[] getKeys() {
        final int[] keys = new int[getSize()];
        int j = 0;
        for (int index = 0; index < table.length; index++) {
            int value = table[index];
            if (value != NULL) {
                keys[j++] = index + tableOffset;
            }
        }
        final Set<Integer> set = map.keySet();
        for (Integer key : set) {
            keys[j++] = key;
        }
        Arrays.sort(keys);
        return keys;
    }

    /**
     * Gets the key/value pairs.
     * The array is sorted in ascending key order.
     * @return the key/value pairs with {@code pairs[i] = {key, value}}
     */
    public int[][] getPairs() {
        final int[] keys = getKeys();
        final int[][] pairs = new int[keys.length][2];
        for (int i = 0; i < keys.length; i++) {
            pairs[i][0] = keys[i];
            pairs[i][1] = getValue(keys[i]);
        }
        return pairs;
    }

    /**
     * Gets the key/value ranges.
     * @return {@code ranges = {{keyMin, keyMax}, {valueMin, valueMax}}}
     */
    public int[][] getRanges() {
        int[] keys = getKeys();
        int keyMin = Integer.MAX_VALUE;
        int keyMax = Integer.MIN_VALUE;
        int valueMin = Integer.MAX_VALUE;
        int valueMax = Integer.MIN_VALUE;
        for (int key : keys) {
            keyMin = Math.min(keyMin, key);
            keyMax = Math.max(keyMax, key);
            final int value = getValue(key);
            valueMin = Math.min(valueMin, value);
            valueMax = Math.max(valueMax, value);
        }
        return new int[][]{{keyMin, keyMax}, {valueMin, valueMax}};
    }

    @Override
    public Object clone() {        
        final IntMap clone = new IntMap(tableOffset, table.length);
        final int[][] pairs = getPairs();
        for (int[] pair : pairs) {
            clone.putValue(pair[0], pair[1]);
        }
        return clone;
    }
}
