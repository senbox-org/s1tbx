package com.bc.ceres.binio.util;

import com.bc.ceres.binio.DataContext;
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

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        delegate.read(context, data, position);
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        delegate.write(context, data, position);
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        return delegate.getMaxPosition();
    }
}