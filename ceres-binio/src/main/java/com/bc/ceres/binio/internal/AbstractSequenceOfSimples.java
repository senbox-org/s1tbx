package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;

abstract class AbstractSequenceOfSimples extends AbstractSequence {
    private final int elementSize;
    private DataAccessor dataAccessor;

    protected AbstractSequenceOfSimples(IOContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent, sequenceType);
        this.elementSize = sequenceType.getElementType().getSize();
    }

    protected abstract Segment getSegment();

    protected abstract int getSegmentOffset();

    public byte getByte(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getByte(getSegment().getData(), getSegmentOffset(index));
    }

    public short getShort(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getShort(getSegment().getData(), getSegmentOffset(index));
    }

    public int getInt(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getInt(getSegment().getData(), getSegmentOffset(index));
    }

    public long getLong(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getLong(getSegment().getData(), getSegmentOffset(index));
    }

    public float getFloat(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getFloat(getSegment().getData(), getSegmentOffset(index));
    }

    public double getDouble(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getDouble(getSegment().getData(), getSegmentOffset(index));
    }

    public void setByte(int index, byte value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setByte(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public void setShort(int index, short value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setShort(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public void setInt(int index, int value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setInt(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public void setLong(int index, long value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setLong(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public void setFloat(int index, float value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setFloat(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public void setDouble(int index, double value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setDouble(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    public SequenceInstance getSequence(int index) {
        throw new DataAccessException();
    }

    public CompoundInstance getCompound(int index) {
        throw new DataAccessException();
    }

    protected int getSegmentOffset(int index) {
        return getSegmentOffset() + index * elementSize;
    }

    private void ensureDataAccessible() throws IOException {
        if (dataAccessor == null) {
            dataAccessor = DataAccessor.getInstance(getSequenceType().getElementType(), getContext().getFormat().getByteOrder());
        }
        if (!isDataAccessible()) {
            if (!isSizeResolved()) {
                resolveSize();
            }
            makeDataAccessible();
        }
    }

    protected abstract boolean isDataAccessible();

    protected abstract void makeDataAccessible() throws IOException;
}