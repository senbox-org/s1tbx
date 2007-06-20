/*
 * $Id: ObjectArray.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
