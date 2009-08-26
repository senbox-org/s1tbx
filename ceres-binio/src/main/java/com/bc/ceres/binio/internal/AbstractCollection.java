package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.DataContext;

import java.io.IOException;


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
        throw new DataAccessException();
    }

    @Override
    public void setByte(byte value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public short getShort() {
        throw new DataAccessException();
    }

    @Override
    public void setShort(short value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public int getInt() {
        throw new DataAccessException();
    }

    @Override
    public void setInt(int value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public long getLong() {
        throw new DataAccessException();
    }

    @Override
    public void setLong(long value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public float getFloat() {
        throw new DataAccessException();
    }

    @Override
    public void setFloat(float value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public double getDouble() {
        throw new DataAccessException();
    }

    @Override
    public void setDouble(double value) throws IOException {
        throw new DataAccessException();
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

//    protected final void ensureDataValid() throws IOException {
//        if (!isDataValid()) {
//            validateData();
//            assertDataValid();
//        }
//    }
//
//    protected void assertDataValid() {
//        if (!isDataValid()) {
//            // todo - improve error message
//            throw new DataAccessException(toString());
//        }
//    }

    protected final void ensureSizeResolved(int index) throws IOException {
        if (!isSizeResolved(index)) {
            resolveSize(index);
            assertSizeResolved(index);
        }
    }

    protected void assertSizeResolved(int index) {
        if (!isSizeResolved(index)) {
            // todo - improve error message
            throw new DataAccessException(this + " at index " + index);
        }
    }
}
