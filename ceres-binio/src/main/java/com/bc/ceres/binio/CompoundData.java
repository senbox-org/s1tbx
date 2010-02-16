package com.bc.ceres.binio;

import java.io.IOException;

/**
 * A compound of members of any type.
 */
public interface CompoundData extends CollectionData {
    /**
     * @return The resolved instance type of the compound.
     */
    @Override
    CompoundType getType();

    /**
     * @return The resolved instance type of the compound.
     *
     * @deprecated since ceres 0.10; use {@link #getType()} instead.
     */
    @Deprecated
    CompoundType getCompoundType();

    int getMemberCount();

    int getMemberIndex(String name);

    byte getByte(String name) throws IOException;

    void setByte(String name, byte value) throws IOException;

    int getUByte(String name) throws IOException;

    void setUByte(String name, int value) throws IOException;

    short getShort(String name) throws IOException;

    void setShort(String name, short value) throws IOException;

    int getUShort(String name) throws IOException;

    void setUShort(String name, int value) throws IOException;

    int getInt(String name) throws IOException;

    void setInt(String name, int value) throws IOException;

    long getUInt(String name) throws IOException;

    void setUInt(String name, long value) throws IOException;

    long getLong(String name) throws IOException;

    void setLong(String name, long value) throws IOException;

    float getFloat(String name) throws IOException;

    void setFloat(String name, float value) throws IOException;

    double getDouble(String name) throws IOException;

    void setDouble(String name, double value) throws IOException;

    SequenceData getSequence(String name) throws IOException;

    CompoundData getCompound(String name) throws IOException;
}
