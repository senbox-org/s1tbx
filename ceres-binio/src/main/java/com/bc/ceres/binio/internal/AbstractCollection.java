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

import java.io.IOException;
import java.text.MessageFormat;


abstract class AbstractCollection implements CollectionInstance {
    private final DataContext context;
    private final CollectionData parent;

    protected AbstractCollection(DataContext context, CollectionData parent) {
        this.context = context;
        this.parent = parent;
    }

    @Override
    public DataContext getContext() {
        return context;
    }

    @Override
    public CollectionData getParent() {
        return parent;
    }

    @Override
    public boolean isSizeResolved() {
        return getSize() >= 0L;
    }

    /////////////////////////////////////////////////////////////////////////
    // Non-indexed data access (not applicable)

    @Override
    public byte getByte() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setByte(byte value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public short getShort() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setShort(short value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public int getInt() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setInt(int value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public long getLong() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setLong(long value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public float getFloat() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setFloat(float value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public double getDouble() {
        throw new DataAccessException(getTypeErrorMsg());
    }

    @Override
    public void setDouble(double value) throws IOException {
        throw new DataAccessException(getTypeErrorMsg());
    }

    /////////////////////////////////////////////////////////////////////////
    // Indexed data access

    @Override
    public int getUByte(int index) throws IOException {
        return getByte(index) & 0xFF;
    }

    @Override
    public int getUShort(int index) throws IOException {
        return getShort(index) & 0xFFFF;
    }

    @Override
    public long getUInt(int index) throws IOException {
        return getInt(index) & 0xFFFFFFFFL;
    }

    @Override
    public void setUByte(int index, int value) throws IOException {
        setInt(index, value);
    }

    @Override
    public void setUShort(int index, int value) throws IOException {
        setInt(index, value);
    }

    @Override
    public void setUInt(int index, long value) throws IOException {
        setLong(index, value);
    }

    /////////////////////////////////////////////////////////////////////////
    // Resolving

    protected final void ensureSizeResolved(int index) throws IOException {
        if (!isSizeResolved(index)) {
            resolveSize(index);
            assertSizeResolved(index);
        }
    }

    protected void assertSizeResolved(int index) {
        if (!isSizeResolved(index)) {
            throw new DataAccessException(this + " at index " + index);
        }
    }

    String getTypeErrorMsg() {
        return MessageFormat.format("Illegal data access, actual type is ''{0}''", getType());
    }

}
