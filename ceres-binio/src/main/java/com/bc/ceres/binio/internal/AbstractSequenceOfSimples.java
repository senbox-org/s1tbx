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

import java.io.IOException;

abstract class AbstractSequenceOfSimples extends AbstractSequence {
    private final int elementSize;
    private DataAccessor dataAccessor;

    protected AbstractSequenceOfSimples(DataContext context, CollectionData parent, SequenceType sequenceType) {
        super(context, parent, sequenceType);
        this.elementSize = sequenceType.getElementType().getSize();
    }

    protected abstract Segment getSegment();

    protected abstract int getSegmentOffset();

    @Override
    public byte getByte(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getByte(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public short getShort(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getShort(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public int getInt(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getInt(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public long getLong(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getLong(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public float getFloat(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getFloat(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public double getDouble(int index) throws IOException {
        ensureDataAccessible();
        return dataAccessor.getDouble(getSegment().getData(), getSegmentOffset(index));
    }

    @Override
    public void setByte(int index, byte value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setByte(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public void setShort(int index, short value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setShort(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public void setInt(int index, int value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setInt(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public void setLong(int index, long value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setLong(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public void setFloat(int index, float value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setFloat(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public void setDouble(int index, double value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setDouble(getSegment().getData(), getSegmentOffset(index), value);
        getSegment().setDirty(true);
    }

    @Override
    public SequenceInstance getSequence(int index) {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public CompoundInstance getCompound(int index) {
        throw new DataAccessException(getTypeErrorMsg());
    }

    protected int getSegmentOffset(int index) {
        return getSegmentOffset() + index * elementSize;
    }

    private void ensureDataAccessible() throws IOException {
        if (dataAccessor == null) {
            dataAccessor = DataAccessor.getInstance(getType().getElementType(), getContext().getFormat().getByteOrder());
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