package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

public class DataContextImpl implements DataContext {
    private final DataFormat format;
    private final IOHandler handler;

    public DataContextImpl(DataFormat format, IOHandler handler) {
        this.format = format;
        this.handler = handler;
    }

    @Override
    public DataFormat getFormat() {
        return format;
    }

    @Override
    public IOHandler getHandler() {
        return handler;
    }

    @Override
    public CompoundData getData() {
        return getData(0L);
    }

    @Override
    public CompoundData getData(long position) {
        return getData(format.getType(), position);
    }

    @Override
    public CompoundData getData(CompoundType type, long position) {
        return InstanceFactory.createCompound(this, null, type, position, format.getByteOrder());
    }

    @Override
    public void dispose() {
    }
}