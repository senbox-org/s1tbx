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