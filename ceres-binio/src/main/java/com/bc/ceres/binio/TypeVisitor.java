package com.bc.ceres.binio;

public interface TypeVisitor {
    void accept(SimpleType type);

    void accept(CompoundType type);

    void accept(SequenceType type);
}
