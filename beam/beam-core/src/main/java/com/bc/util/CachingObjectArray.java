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

public class CachingObjectArray {

    private ObjectFactory _objectFactory;
    private ObjectArray _objectArray;
    private int minIndex, maxIndex;

    public CachingObjectArray(ObjectFactory objectFactory) {
        if (objectFactory == null) {
            throw new IllegalArgumentException("objectFactory == null");
        }
        _objectFactory = objectFactory;
    }

    public ObjectFactory getObjectFactory() {
        return _objectFactory;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        _objectFactory = objectFactory;
    }

    public void setCachedRange(int indexMin, int indexMax) {
        if (indexMax < indexMin) {
            throw new IllegalArgumentException("indexMin < indexMax");
        }
        final ObjectArray objectArray = new ObjectArray(indexMin, indexMax);
        final ObjectArray objectArrayOld = _objectArray;
        if (objectArrayOld != null) {
            objectArray.set(objectArrayOld);
            objectArrayOld.clear();
        }
        _objectArray = objectArray;
        minIndex = _objectArray.getMinIndex();
        maxIndex = _objectArray.getMaxIndex();
    }

    public final Object getObject(final int index) throws Exception {
        if (index < minIndex || index > maxIndex) {
            return _objectFactory.createObject(index);
        }
        Object object = _objectArray.getObject(index);
        if (object == null) {
            object = _objectFactory.createObject(index);
            _objectArray.setObject(index, object);
        }
        return object;
    }

    public final void setObject(final int index, final Object o) {
        final Object object = _objectArray.getObject(index);
        if (object == null) {
             _objectArray.setObject(index, o);
        }
    }

    public void clear() {
        if (_objectArray != null) {
            _objectArray.clear();
        }
    }

    public static interface ObjectFactory {

        Object createObject(int index) throws Exception;
    }
}
