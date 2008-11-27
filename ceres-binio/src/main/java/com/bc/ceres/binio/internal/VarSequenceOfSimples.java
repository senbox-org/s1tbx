package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;


final class VarSequenceOfSimples extends AbstractSequenceOfSimples {
    private SequenceType resolvedSequenceType;
    private Segment segment;
    private final long position;

    public VarSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType);
        this.position = position;
    }

    @Override
    protected Segment getSegment() {
        return segment;
    }

    @Override
    protected int getSegmentOffset() {
        return 0;
    }

    public Type getType() {
        return resolvedSequenceType;
    }

    public long getPosition() {
        return position;
    }

    public long getSize() {
        return resolvedSequenceType != null ? resolvedSequenceType.getSize() : -1L;
    }

    public int getElementCount() {
        return resolvedSequenceType != null ? resolvedSequenceType.getElementCount() : -1;
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    public boolean isSizeResolved(int index) {
        return resolvedSequenceType != null;
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    public void resolveSize(int index) throws IOException {
        resolveSize();
    }

    // todo - code duplication: see VarSequenceOfFixCollections.resolveSize()
    public void resolveSize() throws IOException {
        if (resolvedSequenceType == null) {
            resolvedSequenceType = resolveSequenceType();
            if (!resolvedSequenceType.isSizeKnown()) {
                throw new DataAccessException(toString());
            }
        }
    }

    @Override
    public boolean isDataAccessible() {
        return segment != null && segment.isDataAccessible();
    }

    @Override
    public void makeDataAccessible() throws IOException {
        if (!isDataAccessible()) {
            segment = new Segment(position, resolvedSequenceType.getSize());
            segment.makeDataAccessible(getContext());
        }
    }

    public void flush() throws IOException {
        if (isDataAccessible()) {
            segment.flushData(getContext());
        }
    }
}