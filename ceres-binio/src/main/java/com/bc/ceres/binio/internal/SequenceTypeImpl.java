package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.core.Assert;

/**
 * Represents a data type that is composed of a sequence of zero or more elements
 * all having the same data type.
 */
public final class SequenceTypeImpl extends AbstractType implements SequenceType {
    private final String name;
    private final Type elementType;
    private final int elementCount;
    private final int size;

    public SequenceTypeImpl(Type elementType, int elementCount) {
        Assert.notNull(elementType, "elementCount");
        Assert.argument(elementCount >= 0, "elementCount");
        this.name = elementType.getName() + "[" + elementCount + "]";
        this.elementType = elementType;
        this.elementCount = elementCount;
        this.size = elementCount * elementType.getSize();
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final int getElementCount() {
        return elementCount;
    }

    @Override
    public final String getName() {
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