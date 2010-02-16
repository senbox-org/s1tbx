package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;
import com.bc.ceres.core.Assert;

import java.io.IOException;

public abstract class VarElementCountSequenceType extends AbstractType implements VarSequenceType {
    private final String name;
    private final Type elementType;

    protected VarElementCountSequenceType(Type elementType) {
        this(elementType.getName() + "[]", elementType);
    }

    protected VarElementCountSequenceType(String name, Type elementType) {
        Assert.notNull(name, "name");
        Assert.notNull(elementType, "elementType");
        this.name = name;
        this.elementType = elementType;
    }

    @Override
    public SequenceType resolve(CollectionData parent) throws IOException {
        int elementCount = resolveElementCount(parent);
        return TypeBuilder.SEQUENCE(elementType, elementCount);
    }

    protected abstract int resolveElementCount(CollectionData parent) throws IOException;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final int getSize() {
        return -1;
    }

    @Override
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final int getElementCount() {
        return -1;
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
