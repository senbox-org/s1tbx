package com.bc.ceres.binio;

public final class SimpleType implements Type {
    public final static SimpleType BYTE = new SimpleType("byte", 1);
    public final static SimpleType UBYTE = new SimpleType("ubyte", 1);
    public final static SimpleType SHORT = new SimpleType("short", 2);
    public final static SimpleType USHORT = new SimpleType("ushort", 2);
    public final static SimpleType INT = new SimpleType("int", 4);
    public final static SimpleType UINT = new SimpleType("uint", 4);
    public final static SimpleType LONG = new SimpleType("long", 8);
    public final static SimpleType ULONG = new SimpleType("ulong", 8);
    public final static SimpleType FLOAT = new SimpleType("float", 4);
    public final static SimpleType DOUBLE = new SimpleType("double", 8);

    private final String name;
    private final int size;

    private SimpleType(String name, int size) {
        this.name = name;
        this.size = size;
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
    public final boolean isSizeKnown() {
        return true;
    }

    @Override
    public final boolean isSimpleType() {
        return true;
    }

    @Override
    public final boolean isCollectionType() {
        return false;
    }

    @Override
    public final boolean isSequenceType() {
        return false;
    }

    @Override
    public final boolean isCompoundType() {
        return false;
    }
}
