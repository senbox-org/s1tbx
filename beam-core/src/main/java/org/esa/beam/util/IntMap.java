package org.esa.beam.util;

import java.util.*;

public class IntMap {
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

    public int size() {
        return size;
    }

    public void put(int key, int value) {
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

    public void remove(int key) {
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

    public int get(int key) {
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

    public int[] keys() {
        int j = 0;
        final int[] keys = new int[size()];
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
        return keys;
    }
}
