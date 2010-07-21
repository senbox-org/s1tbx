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
import java.io.RandomAccessFile;
import java.io.EOFException;

public class RandomAccessFileIOHandler implements IOHandler {
    private final RandomAccessFile raf;

    public RandomAccessFileIOHandler(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public synchronized void read(DataContext context, byte[] data, long position) throws IOException {
        synchronized (raf) {
            raf.seek(position);
            // We do not check for EOF here, because read() is called whenever
            // segment data is allocated
            raf.read(data, 0, data.length);
        }
    }

    @Override
    public synchronized void write(DataContext context, byte[] data, long position) throws IOException {
        synchronized (raf) {
            raf.seek(position);
            raf.write(data);
        }
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        synchronized (raf) {
            return raf.length();
        }
    }
}