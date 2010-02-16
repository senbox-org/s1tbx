package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.CompoundData;

import java.io.IOException;

public class ChoiceExpr extends AbstractExpression {
    private final Expression condition;
    private final Expression expression1;
    private final Expression expression2;

    public ChoiceExpr(Expression condition, Expression expression1, Expression expression2) {
        this.condition = condition;
        this.condition.setParent(this);
        this.expression1 = expression1;
        this.expression1.setParent(this);
        this.expression2 = expression2;
        this.expression2.setParent(this);
    }

    public boolean isConstant() {
        return condition.isConstant()
                && expression1.isConstant()
                && expression2.isConstant();
    }

    public Object evaluate(CompoundData context) throws IOException {
        final Object result = this.condition.evaluate(context);
        if (Boolean.TRUE.equals(result)) {
            return expression1.evaluate(context);
        } else {
            return expression2.evaluate(context);
        }
    }
}