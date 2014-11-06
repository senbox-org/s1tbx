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

package com.bc.ceres.binio.util;

import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;

public class ByteArrayIOHandler implements IOHandler {

    private byte[] byteArray;
    private int size;

    public ByteArrayIOHandler() {
        this(new byte[0]);
    }

    public ByteArrayIOHandler(byte[] byteArray) {
        this.byteArray = byteArray;
        this.size = byteArray.length;
    }

    public byte[] toByteArray() {
        return copyByteArray(size);
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        int r = data.length;
        if (position + r > (long) Integer.MAX_VALUE) {
            throw new IOException();
        }
        if (position >= size) {
            return;
        }
        int pos = (int) position;
        int n = Math.min(size - pos, r);
        if (n == 0) {
            return;
        }
        System.arraycopy(byteArray, pos, data, 0, n);
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        int n = data.length;
        if (position + n > (long) Integer.MAX_VALUE) {
            throw new IOException();
        }
        int pos = (int) position;
        ensureCapacity(pos + n);
        System.arraycopy(data, 0, byteArray, pos, n);
        size = pos + n;
    }
    
    @Override
    public long getMaxPosition() {
        return size;
    }

    private void ensureCapacity(int newSize) {
        if (byteArray.length < newSize) {
            final int blockSize = 256 << 10;
            byteArray = copyByteArray(blockSize * ((newSize + blockSize - 1) / blockSize));
        }
    }

    private byte[] copyByteArray(int newCapacity) {
        byte[] newBytes = new byte[newCapacity];
        System.arraycopy(byteArray, 0, newBytes, 0, Math.min(byteArray.length, newCapacity));
        return newBytes;
    }
}
