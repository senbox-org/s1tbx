package com.bc.ceres.binio.util;

import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileIOHandler implements IOHandler {
    private final RandomAccessFile raf;

    public RandomAccessFileIOHandler(RandomAccessFile raf) {
        this.raf = raf;
    }

    public synchronized void read(IOContext context, byte[] data, long position) throws IOException {
        synchronized (raf) {
            raf.seek(position);
            raf.read(data, 0, data.length);
        }
    }

    public synchronized void write(IOContext context, byte[] data, long position) throws IOException {
        synchronized (raf) {
            raf.seek(position);
            raf.write(data);
        }
    }
}