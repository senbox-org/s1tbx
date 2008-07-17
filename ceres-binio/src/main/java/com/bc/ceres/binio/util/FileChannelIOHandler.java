package com.bc.ceres.binio.util;

import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelIOHandler implements IOHandler {
    private final FileChannel fileChannel;

    public FileChannelIOHandler(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    public void read(IOContext context, byte[] data, long position) throws IOException {
        synchronized (fileChannel) {
            fileChannel.read(ByteBuffer.wrap(data), position);
        }
    }

    public void write(IOContext context, byte[] data, long position) throws IOException {
        synchronized (fileChannel) {
            fileChannel.write(ByteBuffer.wrap(data), position);
        }
    }
}