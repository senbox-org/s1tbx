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
            if (raf.read(data, 0, data.length) == -1) {
                throw new EOFException("illegal read at end of file");
            }
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