package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.*;

import java.io.IOException;

public class DataContextImpl implements DataContext {
    private final DataFormat format;
    private final IOHandler handler;
    private volatile CompoundData data;

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
        if (data == null ) {
            synchronized (this) {
                if (data == null ) {
                    data = createData(0L);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundData createData() {
        return createData(0L);
    }

    @Override
    public CompoundData createData(long position) {
        return createData(format.getType(), position);
    }

    @Override
    public CompoundData createData(CompoundType type, long position) {
        return InstanceFactory.createCompound(this, null, type, position, format.getByteOrder());
    }

    @Override
    public CompoundData getData(long position) {
        return createData(format.getType(), position);
    }

    @Override
    public CompoundData getData(CompoundType type, long position) {
        return createData(type, position);
    }

    @Override
    public synchronized void dispose() {
        if (data != null) {
            try {
                data.flush();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}