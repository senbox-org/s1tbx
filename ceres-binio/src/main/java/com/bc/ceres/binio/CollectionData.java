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

package com.bc.ceres.binio;

import java.io.IOException;

/**
 * A collection of elements.
 */
public interface CollectionData {

    CollectionType getType();

    // todo - remove
    void resolveSize() throws IOException;

    DataContext getContext();

    CollectionData getParent();

    long getPosition();

    long getSize();

    int getElementCount();

    /////////////////////////////////////////////////////////////////////////
    // Data access

    byte getByte(int index) throws IOException;

    void setByte(int index, byte value) throws IOException;

    int getUByte(int index) throws IOException;

    void setUByte(int index, int value) throws IOException;

    short getShort(int index) throws IOException;

    void setShort(int index, short value) throws IOException;

    int getUShort(int index) throws IOException;

    void setUShort(int index, int value) throws IOException;

    int getInt(int index) throws IOException;

    void setInt(int index, int value) throws IOException;

    long getUInt(int index) throws IOException;

    void setUInt(int index, long value) throws IOException;

    long getLong(int index) throws IOException;

    void setLong(int index, long value) throws IOException;

    float getFloat(int index) throws IOException;

    void setFloat(int index, float value) throws IOException;

    double getDouble(int index) throws IOException;

    void setDouble(int index, double value) throws IOException;

    SequenceData getSequence(int index) throws IOException;

    CompoundData getCompound(int index) throws IOException;

    void flush() throws IOException;
}
