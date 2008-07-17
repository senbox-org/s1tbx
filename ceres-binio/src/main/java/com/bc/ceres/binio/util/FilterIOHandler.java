package com.bc.ceres.binio.util;

import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;

public abstract class FilterIOHandler implements IOHandler {
    private final IOHandler delegate;

    protected FilterIOHandler(IOHandler delegate) {
        this.delegate = delegate;
    }

    public final IOHandler getDelegate() {
        return delegate;
    }

    public void read(IOContext context, byte[] data, long position) throws IOException {
        delegate.read(context, data, position);
    }

    public void write(IOContext context, byte[] data, long position) throws IOException {
        delegate.write(context, data, position);
    }
}