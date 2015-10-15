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
package org.esa.snap.core.datamodel;

import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class DummyImageInputStream implements ImageInputStream {

// J2SDK 1.4 >>>>
    public void setByteOrder(ByteOrder byteOrder) {
    }

    public ByteOrder getByteOrder() {
        return null;
    }
// <<<< J2SDK 1.4

// Java Image I/O Beta >>>>
//    public void setByteOrder(boolean b) {
//    }
//
//    public boolean getByteOrder() {
//        return false;
//    }
// <<<< Java Image I/O Beta

    public int read() throws IOException {
        return 0;
    }

    public int read(byte[] bytes) throws IOException {
        return 0;
    }

    public int read(byte[] bytes, int i, int i1) throws IOException {
        return 0;
    }

    public void readBytes(IIOByteBuffer buffer, int i) throws IOException {
    }

    public boolean readBoolean() throws IOException {
        return false;
    }

    public byte readByte() throws IOException {
        return 0;
    }

    public int readUnsignedByte() throws IOException {
        return 0;
    }

    public short readShort() throws IOException {
        return 0;
    }

    public int readUnsignedShort() throws IOException {
        return 0;
    }

    public char readChar() throws IOException {
        return 0;
    }

    public int readInt() throws IOException {
        return 0;
    }

    public long readUnsignedInt() throws IOException {
        return 0;
    }

    public long readLong() throws IOException {
        return 0;
    }

    public float readFloat() throws IOException {
        return 0;
    }

    public double readDouble() throws IOException {
        return 0;
    }

    public String readLine() throws IOException {
        return null;
    }

    public String readUTF() throws IOException {
        return null;
    }

    public void readFully(byte[] bytes, int i, int i1) throws IOException {
    }

    public void readFully(byte[] bytes) throws IOException {
    }

    public void readFully(short[] shorts, int i, int i1) throws IOException {
    }

    public void readFully(char[] chars, int i, int i1) throws IOException {
    }

    public void readFully(int[] ints, int i, int i1) throws IOException {
    }

    public void readFully(long[] longs, int i, int i1) throws IOException {
    }

    public void readFully(float[] floats, int i, int i1) throws IOException {
    }

    public void readFully(double[] doubles, int i, int i1) throws IOException {
    }

    public long getStreamPosition() throws IOException {
        return 0;
    }

    public int getBitOffset() throws IOException {
        return 0;
    }

    public void setBitOffset(int i) throws IOException {
    }

    public int readBit() throws IOException {
        return 0;
    }

    public long readBits(int i) throws IOException {
        return 0;
    }

    public long length() throws IOException {
        return 0;
    }

    public int skipBytes(int i) throws IOException {
        return 0;
    }

    public long skipBytes(long l) throws IOException {
        return 0;
    }

    public void seek(long l) throws IOException {
    }

    public void mark() {
    }

    public void reset() throws IOException {
    }

    public void flushBefore(long l) throws IOException {
    }

    public void flush() throws IOException {
    }

    public long getFlushedPosition() {
        return 0;
    }

    public boolean isCached() {
        return false;
    }

    public boolean isCachedMemory() {
        return false;
    }

    public boolean isCachedFile() {
        return false;
    }

    public void close() throws IOException {
    }
}
