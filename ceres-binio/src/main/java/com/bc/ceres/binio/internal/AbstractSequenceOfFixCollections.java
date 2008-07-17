package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


abstract class AbstractSequenceOfFixCollections extends AbstractSequenceOfCollections {

    protected AbstractSequenceOfFixCollections(IOContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType, position);
    }

    public long getSize() {
        final SequenceType type = getResolvedSequenceType();
        return type != null ? type.getSize() : -1;
    }

    public int getElementCount() {
        final SequenceType type = getResolvedSequenceType();
        return type != null ? type.getElementCount() : -1;
    }

    public Type getType() {
        return getResolvedSequenceType();
    }

    protected abstract SequenceType getResolvedSequenceType();

    public SequenceData getSequence(int index) throws IOException {
        ensureSizeResolved(index);
        final Type elementType = getResolvedSequenceType().getElementType();
        if (elementType instanceof SequenceType) {
            final SequenceType sequenceElementType = (SequenceType) elementType;
            return InstanceFactory.createSequence(getContext(), this, sequenceElementType, getPosition() + index * sequenceElementType.getSize(), getContext().getFormat().getByteOrder());
        }
        throw new DataAccessException();
    }

    public CompoundData getCompound(int index) throws IOException {
        ensureSizeResolved(index);
        final Type elementType = getResolvedSequenceType().getElementType();
        if (elementType instanceof CompoundType) {
            final CompoundType compoundElementType = (CompoundType) elementType;
            return InstanceFactory.createCompound(getContext(), this, compoundElementType, getPosition() + index * compoundElementType.getSize(), getContext().getFormat().getByteOrder());
        }
        throw new DataAccessException();
    }
}