package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.MetadataAware;
import com.bc.ceres.binio.Type;


public final class CompoundMemberImpl implements MetadataAware, CompoundMember {
    private final String name;
    private final Type type;
    private Object metadata;

    public CompoundMemberImpl(String name, Type type) {
        this(name, type, null);
    }

    public CompoundMemberImpl(String name, Type type, Object metadata) {
        this.name = name;
        this.type = type;
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
