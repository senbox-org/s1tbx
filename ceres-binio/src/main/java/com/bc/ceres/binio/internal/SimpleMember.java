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


final class SimpleMember implements MemberInstance {
    private final DataContext context;
    private final CollectionData parent;
    private final SimpleType simpleType;
    private final Segment segment;
    private final int segmentOffset;
    private DataAccessor dataAccessor;

    protected SimpleMember(DataContext context,
                           CollectionData parent,
                           SimpleType simpleType,
                           Segment segment,
                           int segmentOffset) {
        this.context = context;
        this.parent = parent;
        this.simpleType = simpleType;
        this.segment = segment;
        this.segmentOffset = segmentOffset;
    }

    public CollectionData getParent() {
        return parent;
    }

    public Segment getSegment() {
        return segment;
    }

    public int getSegmentOffset() {
        return segmentOffset;
    }

    @Override
    public long getPosition() {
        return segment.getPosition() + segmentOffset;
    }

    @Override
    public long getSize() {
        return simpleType.getSize();
    }

    @Override
    public boolean isSizeResolved() {
        return true;
    }

    @Override
    public void resolveSize() {
        // ok
    }

    ////////////////////////////////////////////////////
    // data access

    @Override
    public byte getByte() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getByte(segment.getData(), segmentOffset);
    }

    @Override
    public void setByte(byte value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setByte(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public short getShort() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getShort(segment.getData(), segmentOffset);
    }

    @Override
    public void setShort(short value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setShort(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public int getInt() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getInt(segment.getData(), segmentOffset);
    }

    @Override
    public void setInt(int value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setInt(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public long getLong() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getLong(segment.getData(), segmentOffset);
    }

    @Override
    public void setLong(long value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setLong(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public float getFloat() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getFloat(segment.getData(), segmentOffset);
    }

    @Override
    public void setFloat(float value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setFloat(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public double getDouble() throws IOException {
        ensureDataAccessible();
        return dataAccessor.getDouble(segment.getData(), segmentOffset);
    }

    @Override
    public void setDouble(double value) throws IOException {
        ensureDataAccessible();
        dataAccessor.setDouble(segment.getData(), segmentOffset, value);
        segment.setDirty(true);
    }

    @Override
    public SequenceInstance getSequence() {
        throw new DataAccessException();
    }

    @Override
    public CompoundInstance getCompound() {
        throw new DataAccessException();
    }

    @Override
    public void flush() throws IOException {
        segment.flushData(context);
    }

    // data access
    ////////////////////////////////////////////////////

    private void ensureDataAccessible() throws IOException {
        if (dataAccessor == null) {
            this.dataAccessor = DataAccessor.getInstance(simpleType, context.getFormat().getByteOrder());
            if (!segment.isDataAccessible()) {
                segment.makeDataAccessible(context);
            }
        }
    }
}
