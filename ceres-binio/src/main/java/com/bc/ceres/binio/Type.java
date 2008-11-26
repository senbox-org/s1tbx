package com.bc.ceres.binio;

public abstract class Type {

    Type() {
    }

    public abstract String getName();

    public abstract int getSize();

    public boolean isSizeKnown() {
        return getSize() >= 0;
    }

    public boolean isSimpleType() {
        return false;
    }

    public boolean isCollectionType() {
        return isCompoundType() || isSequenceType();
    }

    public boolean isSequenceType() {
        return false;
    }

    public boolean isCompoundType() {
        return false;
    }

    public abstract void visit(TypeVisitor visitor);

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
