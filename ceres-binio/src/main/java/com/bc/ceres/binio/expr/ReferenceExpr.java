package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;

// todo - allow for path syntax, e.g. "../../Bt_Data_Counter"

public abstract class ReferenceExpr extends AbstractExpression {
    private String name;
    private int index;

    public ReferenceExpr(String name) {
        this.name = name;
    }

    public ReferenceExpr(int index) {
        this.index = index;
    }

    public boolean isConstant() {
        Expression expression = getParent();
        while (expression != null) {
            if (expression instanceof SequenceExpr) {
                return false;
            }
            expression = expression.getParent();
        }
        return true;
    }

    protected int getIndex(CollectionData parent) {
        if (index == -1) {
            index = ((CompoundData) parent).getMemberIndex(name);
        }
        return index;
    }
}