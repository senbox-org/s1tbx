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

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;

import java.io.IOException;


final class VarSequenceOfFixCollections extends AbstractSequenceOfCollections {
    private SequenceType resolvedSequenceType;

    public VarSequenceOfFixCollections(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType, position);
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    @Override
    public boolean isSizeResolved(int index) {
        return resolvedSequenceType != null && resolvedSequenceType.isSizeKnown();
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    @Override
    public void resolveSize(int index) throws IOException {
        resolveSize();
    }

    // todo - code duplication: see VarSequenceOfSimples.resolveSize()
    @Override
    public void resolveSize() throws IOException {
        if (resolvedSequenceType == null) {
            resolvedSequenceType = resolveSequenceType();
            if (!resolvedSequenceType.isSizeKnown()) {
                throw new DataAccessException(toString());
            }
        }
    }

    @Override
    public void flush() throws IOException {
        // todo - flush modified elements
    }

    @Override
    public long getSize() {
        final SequenceType type = resolvedSequenceType;
        return type != null ? type.getSize() : -1;
    }

    @Override
    public int getElementCount() {
        final SequenceType type = resolvedSequenceType;
        return type != null ? type.getElementCount() : -1;
    }

    @Override
    public SequenceType getType() {
        return resolvedSequenceType;
    }

    @Override
    public SequenceData getSequence(int index) throws IOException {
        ensureSizeResolved(index);
        final Type elementType = resolvedSequenceType.getElementType();
        if (elementType instanceof SequenceType) {
            final SequenceType sequenceElementType = (SequenceType) elementType;
            return InstanceFactory.createSequence(getContext(), this, sequenceElementType, getPosition() + index * sequenceElementType.getSize(), getContext().getFormat().getByteOrder());
        }
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public CompoundData getCompound(int index) throws IOException {
        ensureSizeResolved(index);
        final Type elementType = resolvedSequenceType.getElementType();
        if (elementType instanceof CompoundType) {
            final CompoundType compoundElementType = (CompoundType) elementType;
            return InstanceFactory.createCompound(getContext(), this, compoundElementType, getPosition() + index * compoundElementType.getSize(), getContext().getFormat().getByteOrder());
        }
        throw new DataAccessException(getTypeErrorMsg());
    }
}