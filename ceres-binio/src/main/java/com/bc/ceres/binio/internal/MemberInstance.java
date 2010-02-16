package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.Type;

import java.io.IOException;


interface MemberInstance {

    long getPosition();

    long getSize();

    boolean isSizeResolved();

    void resolveSize() throws IOException;

    ////////////////////////////////////////////////////
    // Compound member data access

    byte getByte() throws IOException;

    void setByte(byte value) throws IOException;

    short getShort() throws IOException;

    void setShort(short value) throws IOException;

    int getInt() throws IOException;

    void setInt(int value) throws IOException;

    long getLong() throws IOException;

    void setLong(long value) throws IOException;

    float getFloat() throws IOException;

    void setFloat(float value) throws IOException;

    double getDouble() throws IOException;

    void setDouble(double value) throws IOException;

    SequenceInstance getSequence();

    CompoundInstance getCompound();

    void flush() throws IOException;
}
