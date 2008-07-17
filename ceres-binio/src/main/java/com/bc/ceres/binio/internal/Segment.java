package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.IOContext;

import java.io.IOException;


final class Segment {
    private final long position;
    private final int size;
    private byte[] data;
    private boolean dirty;
    static final int SEGMENT_SIZE_LIMIT = 16 * 1024;

    public Segment(long position, int size) {
        this.position = position;
        this.size = size;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public long getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isDataAccessible() {
        return data != null;
    }

    public void makeDataAccessible(IOContext context) throws IOException {
        if (data == null) {
            data = new byte[size];
            setDirty(false);
            context.getHandler().read(context, data, position);
        }
    }

    public synchronized void flushData(IOContext context) throws IOException {
        if (isDirty()) {
            context.getHandler().write(context, data, position);
            setDirty(false);
        }
    }
}
