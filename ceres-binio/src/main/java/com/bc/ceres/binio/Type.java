package com.bc.ceres.binio;


public interface Type {
    String getName();

    int getSize();

    boolean isSizeKnown();

    boolean isSimpleType();

    boolean isCollectionType();

    boolean isSequenceType();

    boolean isCompoundType();
}
