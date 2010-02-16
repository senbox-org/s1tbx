package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.DataAccessException;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceType;

import java.io.IOException;


abstract class AbstractSequenceOfCollections extends AbstractSequence {

    private final long position;

    protected AbstractSequenceOfCollections(DataContext context, CollectionData parent, SequenceType sequenceType, long position) {
        super(context, parent, sequenceType);
        this.position = position;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public byte getByte(int index) {
        throw new DataAccessException();
    }

    @Override
    public short getShort(int index) {
        throw new DataAccessException();
    }

    @Override
    public int getInt(int index) {
        throw new DataAccessException();
    }

    @Override
    public long getLong(int index) {
        throw new DataAccessException();
    }

    @Override
    public float getFloat(int index) {
        throw new DataAccessException();
    }

    @Override
    public double getDouble(int index) {
        throw new DataAccessException();
    }

    @Override
    public void setByte(int index, byte value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public void setShort(int index, short value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public void setInt(int index, int value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public void setLong(int index, long value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public void setFloat(int index, float value) throws IOException {
        throw new DataAccessException();
    }

    @Override
    public void setDouble(int index, double value) throws IOException {
        throw new DataAccessException();
    }
}