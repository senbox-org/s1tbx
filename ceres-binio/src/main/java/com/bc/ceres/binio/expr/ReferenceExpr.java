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