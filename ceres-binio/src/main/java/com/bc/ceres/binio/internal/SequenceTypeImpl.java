package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class SequenceTypeImpl extends AbstractType implements SequenceType {
    private volatile String name;
    private final Type elementType;
    private final int elementCount;
    private final int size;

    public SequenceTypeImpl(Type elementType) {
        this(elementType, -1);
    }

    public SequenceTypeImpl(Type elementType, int elementCount) {
        this.elementType = elementType;
        this.elementCount = elementCount >= 0 ? elementCount : -1;
        this.size = (elementCount >= 0 && elementType.getSize() >= 0) ? elementCount * elementType.getSize() : -1;
    }

    public final Type getElementType() {
        return elementType;
    }

    public final int getElementCount() {
        return elementCount;
    }

    @Override
    public final String getName() {
        if (name == null) {
            synchronized (this) {
                if (name == null) {
                    name = elementType.getName() + (elementCount >= 0 ? "[" + elementCount + "]" : "[]");
                }
            }
        }
        return name;
    }

    @Override
    public final int getSize() {
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