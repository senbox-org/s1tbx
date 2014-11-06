/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.CompoundData;

import java.io.IOException;
import java.util.ArrayList;

public class SelectionExpr extends AbstractExpression {
    private final Expression condition;
    private final Case[] cases;
    private final Expression defaultExpression;

    public SelectionExpr(Expression condition, Case[] cases) {
        Expression defaultExpression = null;
        ArrayList<Case> caseList = new ArrayList<SelectionExpr.Case>(cases.length);
        for (SelectionExpr.Case aCase : cases) {
            if (aCase instanceof Default) {
                defaultExpression = aCase.expression;
            } else {
                caseList.add(aCase);
            }
        }
        if (defaultExpression == null) {
            throw new IllegalArgumentException("cases");
        }

        this.condition = condition;
        this.condition.setParent(this);
        this.cases = caseList.toArray(new SelectionExpr.Case[caseList.size()]);
        for (Case aCase : this.cases) {
            aCase.expression.setParent(this);

        }
        this.defaultExpression = defaultExpression;
        this.defaultExpression.setParent(this);
    }

    public boolean isConstant() {
        if (!condition.isConstant()) {
            return false;
        }
        for (Case member : cases) {
            if (!member.expression.isConstant()) {
                return false;
            }
        }
        return defaultExpression.isConstant();
    }

    public Object evaluate(CompoundData context) throws IOException {
        final Object result = condition.evaluate(context);
        for (Case c : cases) {
            if (result.equals(c.value)) {
                return c.expression.evaluate(context);
            }
        }
        return defaultExpression.evaluate(context);
    }

    public static class Case {

        private final Object value;
        private final Expression expression;

        public Case(Object value, Expression expression) {
            this.value = value;
            this.expression = expression;
        }
    }

    public static class Default extends Case {
        public Default(Expression expression) {
            super(null, expression);
        }
    }
}