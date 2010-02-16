package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.Type;

public abstract class AbstractType implements Type {

    protected AbstractType() {
    }

    @Override
    public boolean isSizeKnown() {
        return getSize() >= 0;
    }

    @Override
    public boolean isSimpleType() {
        return false;
    }

    @Override
    public boolean isCollectionType() {
        return isCompoundType() || isSequenceType();
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }

    @Override
    public boolean isCompoundType() {
        return false;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",size=" + getSize() + "]";
    }
}
