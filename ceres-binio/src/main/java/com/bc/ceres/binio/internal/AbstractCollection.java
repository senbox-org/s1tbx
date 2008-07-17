package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.IOContext;

import java.io.IOException;


abstract class AbstractCollection implements CollectionInstance {
    private final IOContext context;
    private final CollectionData parent;

    protected AbstractCollection(IOContext context, CollectionData parent) {
        this.context = context;
        this.parent = parent;
    }

    public IOContext getContext() {
        return context;
    }

    public CollectionData getParent() {
        return parent;
    }

    public boolean isSizeResolved() {
        return getSize() >= 0L;
    }

    /////////////////////////////////////////////////////////////////////////
    // Non-indexed data access (not applicable)

    public byte getByte() {
        throw new DataAccessException();
    }

    public void setByte(byte value) throws IOException {
        throw new DataAccessException();
    }

    public short getShort() {
        throw new DataAccessException();
    }

    public void setShort(short value) throws IOException {
        throw new DataAccessException();
    }

    public int getInt() {
        throw new DataAccessException();
    }

    public void setInt(int value) throws IOException {
        throw new DataAccessException();
    }

    public long getLong() {
        throw new DataAccessException();
    }

    public void setLong(long value) throws IOException {
        throw new DataAccessException();
    }

    public float getFloat() {
        throw new DataAccessException();
    }

    public void setFloat(float value) throws IOException {
        throw new DataAccessException();
    }

    public double getDouble() {
        throw new DataAccessException();
    }

    public void setDouble(double value) throws IOException {
        throw new DataAccessException();
    }

    /////////////////////////////////////////////////////////////////////////
    // Indexed data access

    public int getUByte(int index) throws IOException {
        return getByte(index) & 0xFF;
    }

    public int getUShort(int index) throws IOException {
        return getShort(index) & 0xFFFF;
    }

    public long getUInt(int index) throws IOException {
        return getInt(index) & 0xFFFFFFFFL;
    }

    public void setUByte(int index, int value) throws IOException {
        setInt(index, value);
    }

    public void setUShort(int index, int value) throws IOException {
        setInt(index, value);
    }

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
