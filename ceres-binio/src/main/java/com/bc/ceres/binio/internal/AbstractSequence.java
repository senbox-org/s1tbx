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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;
import java.text.MessageFormat;


abstract class AbstractSequence extends AbstractCollection implements SequenceInstance {
    private final SequenceType sequenceType;

    protected AbstractSequence(DataContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent);
        this.sequenceType = sequenceType;
    }

    @Override
    public SequenceType getType() {
        return sequenceType;
    }

    @Override
    @Deprecated
    public final SequenceType getSequenceType() {
        return getType();
    }

    @Override
    public CompoundInstance getCompound() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public SequenceInstance getSequence() {
        return this;
    }

    SequenceType resolveSequenceType() throws IOException {
        SequenceType unresolvedSequenceType = getType();
        if (unresolvedSequenceType.isSizeKnown()) {
            return unresolvedSequenceType;
        }
        final SequenceType resolvedSequenceType = resolveSequenceType(getParent(), unresolvedSequenceType);
        if (resolvedSequenceType == null || !resolvedSequenceType.isSizeKnown()) {
            String msg = MessageFormat.format("Failed to resolve type ''{0}''", unresolvedSequenceType);
            throw new DataAccessException(msg);
        }
        return resolvedSequenceType;
    }

    static SequenceType resolveSequenceType(CollectionData parent, SequenceType sequenceType) throws IOException {
        SequenceType resolvedSequenceType;
        if (sequenceType instanceof VarSequenceType) {
            resolvedSequenceType = ((VarSequenceType) sequenceType).resolve(parent);
            if (resolvedSequenceType == null) {
                throw new IllegalStateException("Sequence type resolved to null: " + sequenceType);
            }
            if (resolvedSequenceType == sequenceType) {
                throw new IllegalStateException("Sequence type resolved to itself: " + sequenceType);
            }
            if (resolvedSequenceType instanceof VarSequenceType) {
                resolvedSequenceType = resolveSequenceType(parent, resolvedSequenceType);
                if (resolvedSequenceType instanceof VarSequenceType) {
                    throw new IllegalStateException("Sequence type resolved to an unresolvable type: " + resolvedSequenceType);
                }
            }
        }  else {
            resolvedSequenceType = sequenceType;
        }
        return resolvedSequenceType;
    }
}