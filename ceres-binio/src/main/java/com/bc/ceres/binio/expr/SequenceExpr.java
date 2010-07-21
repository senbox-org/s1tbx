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
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.internal.SequenceTypeImpl;

import java.io.IOException;

public class SequenceExpr extends AbstractExpression {
    private final Expression elementType;
    private final Expression elementCount;

    public SequenceExpr(Expression elementType, Expression elementCount) {
        this.elementType = elementType;
        this.elementType.setParent(this);
        this.elementCount = elementCount;
        this.elementCount.setParent(this);
    }

    public boolean isConstant() {
        return elementType.isConstant() && elementCount.isConstant();
    }

    public Object evaluate(CompoundData context) throws IOException {
        if (elementType.isConstant() && elementCount.isConstant()) {
            // return FixEtFixEcSeqType((Type)elementType.evaluate(parent),
            //                          (Integer)elementCount.evaluate(parent));
        } else if (elementType.isConstant()) {
            // return FixEtVarEcSeqType((Type)elementType.evaluate(parent),
            //                          elementCount);
        } else if (elementCount.isConstant()) {
            // return VarEtFixEcSeqType(elementType,
            //                          (Integer)elementCount.evaluate(parent));
        } else {
            // return VarEtVarEcSeqType(elementType,
            //                          (Integer)elementCount.evaluate(parent));
        }

        // todo - wrong child parent used here, parent for children must be instance of "this" sequence
        final Type elementType = (Type) this.elementType.evaluate(context);
        final int elementCount = ((Number) this.elementCount.evaluate(context)).intValue();
        return new SequenceTypeImpl(elementType, elementCount);
    }
}