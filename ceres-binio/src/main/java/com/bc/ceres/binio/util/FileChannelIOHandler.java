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