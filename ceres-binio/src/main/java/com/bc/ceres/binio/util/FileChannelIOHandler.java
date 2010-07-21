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
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelIOHandler implements IOHandler {
    private final FileChannel fileChannel;

    public FileChannelIOHandler(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        synchronized (fileChannel) {
            // We do not check for EOF here, because read() is called whenever
            // segment data is allocated
            fileChannel.read(ByteBuffer.wrap(data), position);
        }
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        synchronized (fileChannel) {
            fileChannel.write(ByteBuffer.wrap(data), position);
        }
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        synchronized (fileChannel) {
            return fileChannel.size();
        }
    }
}