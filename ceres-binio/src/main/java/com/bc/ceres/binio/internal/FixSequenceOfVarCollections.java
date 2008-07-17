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

    FixSequenceOfVarCollections(IOContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType, position);
        unresolvedElementType = sequenceType.getElementType();
        if (unresolvedElementType.isSizeKnown()) {
            throw new IllegalArgumentException("sequenceType");
        }
        maxResolvedElementIndex = -1;
        lastAccessedElementIndex = -1;
        size = -1L;
    }

    public long getSize() {
        return size;
    }

    public Type getType() {
        return getSequenceType();
    }

    public boolean isSizeResolved(int index) {
        return index <= maxResolvedElementIndex;
    }

    public void resolveSize() throws IOException {
        resolveSize(getElementCount() - 1);
    }

    public void resolveSize(int index) throws IOException {
        if (isSizeResolved(index)) {
            return;
        }

        ensureElementOffsetsCreated();

        final int startIndex = maxResolvedElementIndex + 1;

        CollectionInstance elementInstance = null;
        for (int i = startIndex; i <= index; i++) {
            if (i == lastAccessedElementIndex) {
                elementInstance = lastAccessedElementInstance;
            } else {
                elementInstance = createElementInstance(elementOffsets[i]);
            }
            if (!elementInstance.isSizeResolved()) {
                elementInstance.resolveSize();
            }
            elementOffsets[i + 1] = elementOffsets[i] + elementInstance.getSize();
        }
        if (index == getElementCount() - 1) {
            size = elementOffsets[getElementCount()] - elementOffsets[0];
        }

        maxResolvedElementIndex = index;
        maxResolvedElementInstance = elementInstance;
    }

    private void ensureElementOffsetsCreated() {
        if (elementOffsets == null) {
            elementOffsets = new long[getElementCount() + 1];
            Arrays.fill(elementOffsets, -1L);
            elementOffsets[0] = getPosition();
        }
    }

    public int getElementCount() {
        return getSequenceType().getElementCount();
    }

    public SequenceData getSequence(int index) throws IOException {
        return (SequenceData) createElementInstance(index);
    }

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

    public void flush() throws IOException {
        // todo - flush modified elements
    }
}
