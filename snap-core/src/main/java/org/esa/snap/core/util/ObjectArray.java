/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

public final class ObjectArray {

    private final int minIndex;
    private final int maxIndex;
    private final Object[] objects;

    public ObjectArray(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("max must be greater than or equal min");
        }
        minIndex = min;
        maxIndex = max;
        objects = new Object[max - min + 1];
    }

    public int getMinIndex() {
        return minIndex;
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public Object getObject(int i) {
        return objects[i - minIndex];
    }

    public void setObject(int i, Object o) {
        objects[i - minIndex] = o;
    }

    public void clear() {
        Arrays.fill(objects, 0, objects.length, null);
    }

    private int getArrayIndex(int i) {
        return i - minIndex;
    }

    public void set(ObjectArray array) {
        final int start = Math.max(minIndex, array.getMinIndex());
        final int end = Math.min(maxIndex, array.getMaxIndex());

        if (end < start) {
            return;
        }

        final int srcPos = start - array.getMinIndex();
        final int destPos = start - minIndex;
        System.arraycopy(array.objects, srcPos, objects, destPos, end - start + 1);
    }
}
