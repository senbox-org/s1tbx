package com.bc.ceres.binio.expr;

/**
 * todo - API doc
 */
public abstract class AbstractExpression implements Expression {
    private Expression parent;

    public Expression getParent() {
        return parent;
    }

    public void setParent(Expression parent) {
        this.parent = parent;
    }
}
