package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.CompoundData;

import java.io.IOException;

public class ConstantExpr extends AbstractExpression {
    private final Expression expression;
    private volatile Object value;

    public ConstantExpr(Object value) {
        this.expression = null;
        this.value = value;
    }

    public ConstantExpr(Expression expression) {
        this.expression = expression;
        this.expression.setParent(this);
    }

    public boolean isConstant() {
        return true;
    }

    public Object evaluate(CompoundData context) throws IOException {
        if (value == null && expression != null) {
            synchronized (this) {
                if (value == null) {
                    value = expression.evaluate(context);
                }
            }
        }
        return value;
    }
}