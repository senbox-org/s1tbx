/*
 * $Id: CachingObjectArray.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.util;

public class CachingObjectArray {

    private ObjectFactory _objectFactory;
    private ObjectArray _objectArray;

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
    }

    public Object getObject(int index) throws Exception {
        Object object;
        if (index < _objectArray.getMinIndex() || index > _objectArray.getMaxIndex()) {
            object = createObject(index);
        } else {
            object = _objectArray.getObject(index);
            if (object == null) {
                object = createObject(index);
                _objectArray.setObject(index, object);
            }
        }
        return object;
    }

    private Object createObject(int index) throws Exception {
        return _objectFactory.createObject(index);
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
