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
package com.bc.util;

import java.util.Arrays;

public class ObjectArray {

    private final int _minIndex;
    private final int _maxIndex;
    private final Object[] _objects;

    public ObjectArray(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("max must be greater than or equal min");
        }
        _minIndex = min;
        _maxIndex = max;
        _objects = new Object[max - min + 1];
    }

    public int getMinIndex() {
        return _minIndex;
    }

    public int getMaxIndex() {
        return _maxIndex;
    }

    public Object getObject(int i) {
        return _objects[getArrayIndex(i)];
    }

    public void setObject(int i, Object o) {
        _objects[getArrayIndex(i)] = o;
    }

    public void clear() {
        Arrays.fill(_objects, 0, _objects.length, null);
    }

    private int getArrayIndex(int i) {
        return i - _minIndex;
    }

    public void set(ObjectArray array) {
        final int start = Math.max(getMinIndex(), array.getMinIndex());
        final int end = Math.min(getMaxIndex(), array.getMaxIndex());

        if (end < start) {
            return;
        }

        final int srcPos = start - array.getMinIndex();
        final int destPos = start - getMinIndex();
        final int length = end - start + 1;
        System.arraycopy(array._objects, srcPos, _objects, destPos, length);
    }
}
