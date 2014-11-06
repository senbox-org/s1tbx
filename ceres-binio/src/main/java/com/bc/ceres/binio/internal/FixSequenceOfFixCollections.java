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
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;

import java.io.IOException;
import java.text.MessageFormat;


final class FixSequenceOfFixCollections extends AbstractSequenceOfCollections {

    private final Segment segment;

    public FixSequenceOfFixCollections(DataContext context,
                                       CollectionData parent,
                                       SequenceType sequenceType,
                                       long position) {
        super(context, parent, sequenceType, position);
        if (!sequenceType.isCollectionType() || !sequenceType.isSizeKnown()) {
            throw new IllegalArgumentException("sequenceType");
        }
        if (sequenceType.getSize() <= Segment.getSegmentSizeLimit()) {
            this.segment = new Segment(position, sequenceType.getSize());
        } else {
            this.segment = null;
        }
    }

    public FixSequenceOfFixCollections(DataContext context,
                                       CollectionData parent,
                                       SequenceType sequenceType,
                                       Segment segment,
                                       int segmentOffset) {
        super(context, parent, sequenceType, segment.getPosition() + segmentOffset);
        this.segment = null;
    }

    @Override
    public long getSize() {
        return getType().getSize();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
    }

    @Override
    public int getElementCount() {
        return getType().getElementCount();
    }

    @Override
    public boolean isSizeResolved(int index) {
        return true;
    }

    @Override
    public void resolveSize(int index) throws IOException {
        // ok
    }

    @Override
    public void resolveSize() throws IOException {
        // ok
    }

    @Override
    public SequenceData getSequence(int index) throws IOException {
        final Type elementType = getType().getElementType();
        if (elementType instanceof SequenceType) {
            final SequenceType sequenceElementType = (SequenceType) elementType;
            if (segment != null) {
                return InstanceFactory.createFixSequence(getContext(), this, sequenceElementType, segment,
                                                         index * sequenceElementType.getSize());
            } else {
                return InstanceFactory.createSequence(getContext(), this, sequenceElementType,
                                                      getPosition() + index * sequenceElementType.getSize(),
                                                      getContext().getFormat().getByteOrder());
            }
        }
        throw new DataAccessException(MessageFormat.format("Sequence expected at index = {0}", index));
    }

    @Override
    public CompoundData getCompound(int index) throws IOException {
        final Type elementType = getType().getElementType();
        if (elementType instanceof CompoundType) {
            final CompoundType compoundElementType = (CompoundType) elementType;
            if (segment != null) {
                return InstanceFactory.createFixCompound(getContext(), this, compoundElementType, segment,
                                                         index * compoundElementType.getSize());
            } else {
                return InstanceFactory.createCompound(getContext(), this, compoundElementType,
                                                      getPosition() + index * compoundElementType.getSize(),
                                                      getContext().getFormat().getByteOrder());
            }
        }
        throw new DataAccessException(MessageFormat.format("Compound expected at index = {0}", index));
    }

    @Override
    public void flush() throws IOException {
        if (segment != null) {
            segment.flushData(getContext());
        } else {
            // todo - flush modified elements
        }
    }

}