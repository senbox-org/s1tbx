package com.bc.ceres.binio;


public interface SequenceType extends CollectionType {
    Type getElementType();

    int getElementCount();
}
