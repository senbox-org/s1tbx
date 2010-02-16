package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.CompoundData;

import java.io.IOException;

public interface Expression {
    boolean isConstant();

    Object evaluate(CompoundData context) throws IOException;

    Expression getParent();

    void setParent(Expression parent);
}
