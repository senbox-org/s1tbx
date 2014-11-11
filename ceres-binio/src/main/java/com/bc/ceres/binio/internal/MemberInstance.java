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
