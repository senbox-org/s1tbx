package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


final class FixSequenceOfFixCollections extends AbstractSequenceOfFixCollections {

    private final Segment segment;

    public FixSequenceOfFixCollections(DataContext context,
                                       CollectionData parent,
                                       SequenceType sequenceType,
                                       long position) {
        super(context, parent, sequenceType, position);
        if (!sequenceType.isCollectionType() || !sequenceType.isSizeKnown()) {
            throw new IllegalArgumentException("sequenceType");
        }
        if (sequenceType.getSize() <= com.bc.ceres.binio.internal.Segment.SEGMENT_SIZE_LIMIT) {
            this.segment = new Segment(position, sequenceType.getSize());
        } else {
            this.segment = null;
        }
    }

    public FixSequenceOfFixCollections(DataContext context,
                                       CollectionData parent,
                                       SequenceType sequenceType,
                                       Segment segment,
                                       int segmentOffset
    ) {
        super(context, parent, sequenceType, segment.getPosition() + segmentOffset);
        this.segment = null;
    }

    @Override
    public long getSize() {
        return getSequenceType().getSize();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
    }

    @Override
    public Type getType() {
        return getSequenceType();
    }

    @Override
    protected SequenceType getResolvedSequenceType() {
        return getSequenceType();
    }

    @Override
    public int getElementCount() {
        return getSequenceType().getElementCount();
    }

    public boolean isSizeResolved(int index) {
        return true;
    }

    public void resolveSize(int index) throws IOException {
        // ok
    }

    public void resolveSize() throws IOException {
        // ok
    }

    @Override
    public SequenceData getSequence(int index) throws IOException {
        if (segment != null) {
            ensureSizeResolved(index);
            final Type elementType = getResolvedSequenceType().getElementType();
            if (elementType instanceof SequenceType) {
                final SequenceType sequenceElementType = (SequenceType) elementType;
                return InstanceFactory.createFixSequence(getContext(), this, sequenceElementType, segment, index * sequenceElementType.getSize());
            }
            throw new DataAccessException();
        } else {
            return super.getSequence(index);
        }
    }

    @Override
    public CompoundData getCompound(int index) throws IOException {
        if (segment != null) {
            ensureSizeResolved(index);
            final Type elementType = getResolvedSequenceType().getElementType();
            if (elementType instanceof CompoundType) {
                final CompoundType compoundElementType = (CompoundType) elementType;
                return InstanceFactory.createFixCompound(getContext(), this, compoundElementType, segment, index * compoundElementType.getSize());
            }
            throw new DataAccessException();
        } else {
            return super.getCompound(index);
        }
    }

    public void flush() throws IOException {
        if (segment != null) {
            segment.flushData(getContext());
        } else {
            // todo - flush modified elements
        }
    }

}