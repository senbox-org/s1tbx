package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.MetadataAware;
import com.bc.ceres.binio.Type;


public final class CompoundMemberImpl implements MetadataAware, CompoundMember {
    private final String name;
    private final Type type;
    private final long size;
    private Object metadata;

    public CompoundMemberImpl(String name, Type type) {
        this(name, type, null);
    }

    public CompoundMemberImpl(String name, Type type, Object metadata) {
        this(name, type, type.getSize(), metadata);
    }

    public CompoundMemberImpl(String name, Type type, long size, Object metadata) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.metadata = metadata;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getSize() {
        return size == -1 ? type.getSize() : size;
    }

    @Override
    public Object getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
