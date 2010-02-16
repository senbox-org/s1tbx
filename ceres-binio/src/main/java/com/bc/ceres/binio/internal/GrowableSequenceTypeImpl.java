package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class GrowableSequenceTypeImpl extends AbstractType implements SequenceType {
    private final String name;
    private final Type elementType;
    private int elementCount;
    private int size;

    public GrowableSequenceTypeImpl(Type elementType) {
        name = elementType.getName() + "[]";
        this.elementType = elementType;
        this.elementCount = 0;
        this.size = 0;
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final synchronized int getElementCount() {
        return elementCount;
    }

    public final synchronized void incElementCount() {
        this.elementCount++;
        this.size += elementType.getSize();
    }

    @Override
    public final synchronized String getName() {
        return name;
    }

    @Override
    public final synchronized int getSize() {
        return size;
    }

    @Override
    public final boolean isCollectionType() {
        return true;
    }

    @Override
    public final boolean isSequenceType() {
        return true;
    }
}