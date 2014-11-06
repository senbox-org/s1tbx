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
import java.util.Arrays;


final class FixSequenceOfVarCollections extends AbstractSequenceOfCollections {
    private final Type unresolvedElementType;
    private long[] elementOffsets;
    private int maxResolvedElementIndex;
    private CollectionInstance maxResolvedElementInstance;
    private int lastAccessedElementIndex;
    private CollectionInstance lastAccessedElementInstance;
    private long size;

    FixSequenceOfVarCollections(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType, position);
        unresolvedElementType = sequenceType.getElementType();
        if (unresolvedElementType.isSizeKnown()) {
            throw new IllegalArgumentException("sequenceType");
        }
        maxResolvedElementIndex = -1;
        lastAccessedElementIndex = -1;
        size = -1L;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean isSizeResolved(int index) {
        return index <= maxResolvedElementIndex;
    }

    @Override
    public void resolveSize() throws IOException {
        resolveSize(getElementCount() - 1);
    }

    @Override
    public void resolveSize(int index) throws IOException {
        if (isSizeResolved(index)) {
            return;
        }

        ensureElementOffsetsCreated();

        CollectionInstance elementInstance = null;
        for (int i = maxResolvedElementIndex + 1; i <= index; i++) {
            if (i == lastAccessedElementIndex) {
                elementInstance = lastAccessedElementInstance;
            } else {
                elementInstance = createElementInstance(elementOffsets[i]);
            }
            if (!elementInstance.isSizeResolved()) {
                elementInstance.resolveSize();
            }
            elementOffsets[i + 1] = elementOffsets[i] + elementInstance.getSize();
            maxResolvedElementIndex = i;
            maxResolvedElementInstance = elementInstance;
        }
        if (index == getElementCount() - 1) {
            size = elementOffsets[getElementCount()] - elementOffsets[0];
        }
    }

    private void ensureElementOffsetsCreated() {
        if (elementOffsets == null) {
            elementOffsets = new long[getElementCount() + 1];
            Arrays.fill(elementOffsets, -1L);
            elementOffsets[0] = getPosition();
        }
    }

    @Override
    public int getElementCount() {
        return getType().getElementCount();
    }

    @Override
    public SequenceData getSequence(int index) throws IOException {
        return (SequenceData) createElementInstance(index);
    }

    @Override
    public CompoundData getCompound(int index) throws IOException {
        return (CompoundData) createElementInstance(index);
    }

    private synchronized CollectionInstance createElementInstance(int index) throws IOException {
        if (index > 0) {
            ensureSizeResolved(index - 1);
        } else {
            ensureElementOffsetsCreated();
        }
        if (index == lastAccessedElementIndex) {
            return lastAccessedElementInstance;
        }
        CollectionInstance elementInstance;
        if (index == maxResolvedElementIndex) {
            elementInstance = maxResolvedElementInstance;
        } else {
            elementInstance = createElementInstance(elementOffsets[index]);
        }
        lastAccessedElementIndex = index;
        lastAccessedElementInstance = elementInstance;
        return elementInstance;
    }

    private CollectionInstance createElementInstance(long position) throws IOException {
        return InstanceFactory.createCollection(getContext(), this, unresolvedElementType, position, getContext().getFormat().getByteOrder());
    }

    @Override
    public void flush() throws IOException {
        // todo - flush modified elements
    }
}
