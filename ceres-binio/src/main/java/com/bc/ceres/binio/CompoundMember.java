package com.bc.ceres.binio;

public interface CompoundMember extends MetadataAware {
    String getName();

    Type getType();

    long getSize();
}
