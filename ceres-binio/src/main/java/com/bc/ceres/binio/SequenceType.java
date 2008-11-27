package com.bc.ceres.binio;


public interface SequenceType extends Type {
    Type getElementType();

    int getElementCount();
}
