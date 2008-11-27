package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;

import java.io.IOException;


final class FixSequenceOfSimples extends AbstractSequenceOfSimples {
    private final Segment segment;
    private final int segmentOffset;

    public FixSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        this(context, parent, sequenceType, new Segment(position, sequenceType.getSize()), 0);
    }

    public FixSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType, Segment segment, int segmentOffset) {
        super(context, parent, sequenceType);
        this.segment = segment;
        this.segmentOffset = segmentOffset;
    }

    @Override
    protected Segment getSegment() {
        return segment;
    }

    @Override
    protected int getSegmentOffset() {
        return segmentOffset;
    }

    public long getPosition() {
        return segment.getPosition();
    }

    public long getSize() {
        return getSequenceType().getSize();
    }

    public int getElementCount() {
        return getSequenceType().getElementCount();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
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

    public Type getType() {
        return getSequenceType();
    }

    @Override
    public boolean isDataAccessible() {
        return segment.isDataAccessible();
    }

    @Override
    public void makeDataAccessible() throws IOException {
        segment.makeDataAccessible(getContext());
    }

    public void flush() throws IOException {
        segment.flushData(getContext());
    }
}
