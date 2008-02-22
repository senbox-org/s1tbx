package org.esa.beam.util;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class IntMap {
    public static final int NULL = Integer.MIN_VALUE;
    private final Map<Integer, Integer> map;
    private final int[] buffer;
    private final int bufferOffset;
    private int size;

    public IntMap() {
        this(0, 1024);
    }

    public IntMap(int bufferOffset, int bufferSize) {
        map = new TreeMap<Integer, Integer>();
        buffer = new int[bufferSize];
        this.bufferOffset = bufferOffset;
        this.size = 0;
        Arrays.fill(buffer, NULL);
    }

    public int size() {
        return size;
    }

    public void put(int key, int value) {
        if (value == NULL) {
            throw new IllegalArgumentException("value");
        }
        final int i = key - bufferOffset;
        if (i >= 0 && i < buffer.length) {
            final int oldValue = buffer[i];
            buffer[i] = value;
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
        final int i = key - bufferOffset;
        if (i >= 0 && i < buffer.length) {
            final int oldValue = buffer[i];
            buffer[i] = NULL;
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
        final int i = key - bufferOffset;
        if (i >= 0 && i < buffer.length) {
            return buffer[i];
        } else {
            final Integer oldValue = map.get(key);
            if (oldValue != null) {
                return oldValue;
            } else {
                return NULL;
            }
        }
    }
}
