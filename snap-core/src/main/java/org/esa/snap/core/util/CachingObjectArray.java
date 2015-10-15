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

public class CachingObjectArray {

    private ObjectFactory objectFactory;
    private ObjectArray objectArray;
    private int minIndex, maxIndex;

    public CachingObjectArray(ObjectFactory objectFactory) {
        if (objectFactory == null) {
            throw new IllegalArgumentException("objectFactory == null");
        }
        this.objectFactory = objectFactory;
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public void setCachedRange(int indexMin, int indexMax) {
        if (indexMax < indexMin) {
            throw new IllegalArgumentException("indexMin < indexMax");
        }
        final ObjectArray objectArray = new ObjectArray(indexMin, indexMax);
        final ObjectArray objectArrayOld = this.objectArray;
        if (objectArrayOld != null) {
            objectArray.set(objectArrayOld);
            objectArrayOld.clear();
        }
        this.objectArray = objectArray;
        minIndex = this.objectArray.getMinIndex();
        maxIndex = this.objectArray.getMaxIndex();
    }

    public final Object getObject(final int index) throws Exception {
        if (index < minIndex || index > maxIndex) {
            return objectFactory.createObject(index);
        }
        Object object = objectArray.getObject(index);
        if (object == null) {
            object = objectFactory.createObject(index);
            objectArray.setObject(index, object);
        }
        return object;
    }

    public final void setObject(final int index, final Object o) {
        final Object object = objectArray.getObject(index);
        if (object == null) {
            objectArray.setObject(index, o);
        }
    }

    public void clear() {
        if (objectArray != null) {
            objectArray.clear();
        }
    }

    public static interface ObjectFactory {

        Object createObject(int index) throws Exception;
    }
}
