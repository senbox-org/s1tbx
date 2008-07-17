package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;


abstract class AbstractSequenceOfCollections extends AbstractSequence {

    private final long position;

    protected AbstractSequenceOfCollections(IOContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType);
        this.position = position;
    }

    public long getPosition() {
        return position;
    }

    public byte getByte(int index) {
        throw new DataAccessException();
    }

    public short getShort(int index) {
        throw new DataAccessException();
    }

    public int getInt(int index) {
        throw new DataAccessException();
    }

    public long getLong(int index) {
        throw new DataAccessException();
    }

    public float getFloat(int index) {
        throw new DataAccessException();
    }

    public double getDouble(int index) {
        throw new DataAccessException();
    }

    public void setByte(int index, byte value) throws IOException {
        throw new DataAccessException();
    }

    public void setShort(int index, short value) throws IOException {
        throw new DataAccessException();
    }

    public void setInt(int index, int value) throws IOException {
        throw new DataAccessException();
    }

    public void setLong(int index, long value) throws IOException {
        throw new DataAccessException();
    }

    public void setFloat(int index, float value) throws IOException {
        throw new DataAccessException();
    }

    public void setDouble(int index, double value) throws IOException {
        throw new DataAccessException();
    }
}